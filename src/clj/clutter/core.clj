(ns clutter.core
  (:require [clojure.core.async :refer [<! >! go go-loop] :as async]
            [clojure.string :as str]

            [environ.core :refer [env]]

            [chord.http-kit :refer [with-channel]]

            [org.httpkit.server :refer [run-server]]

            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as response]

            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]

            [compojure.core :refer [defroutes GET POST rfn]]
            [compojure.route :refer [resources]]

            [taoensso.timbre :as timbre :refer [info warn error]]
            [taoensso.timbre.appenders.core :as appenders]

            [clutter.commands :as cmd]
            [clutter.config :as config]
            [clutter.db :as db]
            [clutter.messages :as msg]
            [clutter.templates :as t]
            [clutter.utils :as $ :refer [wrap-exceptions wrap-log]]))

(timbre/merge-config!
 {:level :info
  :appenders
  {:spit-appender
   (appenders/spit-appender {:fname "log/clutter.log"})}})



(def commands
  {"say" cmd/do-say
   "edit" cmd/do-edit
   "pose" cmd/do-pose
   "page" cmd/do-page
   "spoof" cmd/do-spoof

   "desc" cmd/do-desc

   "delete" cmd/do-delete
   "rename" cmd/do-rename
   "dig" cmd/do-dig
   "create" cmd/do-create
   "link" cmd/do-link})

(defn process-message
  [user s]
  (let [[handler arg] (condp re-find s
                        #"^\"" [(get commands "say") (str/triml (subs s 1))]
                        #"^:" [(get commands "pose") (str/triml (subs s 1))]

                        ;; default:
                        (let [[cmd arg] (str/split s #" " 2)]
                          [(get commands cmd) arg]))]
    (if handler
      (handler user arg)
      (db/tell user {:type :message
                     :text "Huh?"}))))

(defn do-use
  [user what-id]
  (info "USE:" (db/name user) "attempting to use #" what-id)
  (if-let [what (db/db-get what-id)]
    (if (db/can-use? user what)
      (case (db/dbtype what)
        :exit (let [to (:goto @what)
                    from (db/location what)]
                (when to
                  (db/set-location! user to))
                (db/tell user {:text (or (db/get-prop what :success)
                                         (str  "You use '" (db/name what) "'."))})
                (db/notify from {:text (str (db/name user) " "
                                            (or (db/get-prop what :osuccess)
                                                (str "leaves through '" (db/name what) "'.")))})
                (db/notify-except
                 to {:text (str (db/name user) " "
                                (or (db/get-prop what :othrough)
                                    (str "arrives from '" (db/name from) "'.")))}
                 #{user}))

        (db/tell user {:text "That is not useable."}))
      (db/tell user {:text "You can't use that."}))
    (db/tell user {:text "I don't know what that is."})))

(defn do-queries
  [user q]
  (try
    ;; Although the result of the query does not represent an actual
    ;; change, send it as a db-delta to preserve the other keys in the
    ;; client db-cache.
    {:dbd (db/query-with-perms user q)}
    (catch Exception exc
      {:log-error (str "Bad query:" q)})))

(defn request-user [req]
  (some-> req friend/identity friend/current-authentication))

(defn connect
  [{:keys [params] :as req}]
  (let [user (when-let [uname (:name (request-user req))]
               (db/get-user uname))]
    (with-channel req ws-ch nil
      (if user
        (do
          (info "CONNECTED:" name "-" user)
          (db/add-connection! user ws-ch)
          (go
            ;; Tell the user who s/he is:
            (>! ws-ch {:user-id (db/id user)
                       :text (format config/connect-message (db/name user))
                       :dbd (db/auto-subscribed-summary user)})

            ;; Message loop
            (loop []
              (when-let [{m :message} (<! ws-ch)]
                (info "RECEIVED from" name ":" m)

                (try
                  (case (:type m)
                    :message (process-message user (:text m))
                    :use (do-use user (:what m))

                    (when-let [dbq (:dbq m)]
                      (>! ws-ch (do-queries user dbq))))
                  (catch Exception exc
                    (info exc)
                    (>! ws-ch {:error (str "Uh-oh! Something bad happened:" exc)})))
                (recur)))

            ;; All done
            (db/remove-connection! user ws-ch)
            (db/notify (db/location user) (msg/disconnected user))
            (info "DISCONNECTED:" name)))

        ;; No user:
        (>! ws-ch {:error (str "Invalid user credentials.")})))))

(defn login-view [req]
  {:body (t/login-page (:params req))})

(defn unauthorized-view [_]
  {:status 403
   :body (rand-nth
          ["We would not be so bold."
           "Your audacity has been noted."
           "Your delusions of grandeur have been duly noted."])})

(defn index-view [{:keys [session] :as req}]
  (let [user (when-let [uname (some-> req
                                      friend/identity
                                      friend/current-authentication
                                      :name)]
                  (db/get-user uname))]
    (if user
      (response/file-response "resources/public/index.html")

      (response/redirect "/login"))))

(defroutes clutter
  (GET "/" req (friend/authenticated (index-view req)))
  (GET "/test" req (friend/authenticated (str "Success, " (-> req friend/identity
                                                              friend/current-authentication
                                                              :name))))
  (GET "/login" req (friend/logout login-view))
  (GET "/logout" req (friend/logout* (response/redirect "/login")))

  ;; Web socket:
  (GET "/connect" req (connect req))
  (resources "/"))

(def app (-> clutter
             wrap-log
             (friend/authenticate {:allow-anon? true
                                   :login-uri "/login"
                                   :default-landing-uri "/"
                                   :credential-fn db/authorize-or-create-user
                                   :unauthorized-handler unauthorized-view
                                   :workflows [(workflows/interactive-form)]})
             wrap-session
             wrap-keyword-params
             wrap-params
             wrap-exceptions))

(defn -main [& [cmd & args]]
  (let [port (:port env 3337)]
    (run-server #'app {:port port})
    (info "Started server on port " port)))
