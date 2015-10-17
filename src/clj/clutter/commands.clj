(ns clutter.commands
  (:require [clojure.string :as str]

            [taoensso.timbre :as timbre :refer [info warn error]]

            [clutter.config :as config]
            [clutter.db :as db]
            [clutter.messages :as msg]))

(defn do-say [u arg]
  (db/otell-all u {:type :message
                   :user (db/id u)
                   :text (str " says, \"" arg "\"")}))

(defn do-pose [u arg]
  (db/otell-all u {:type :message
                   :user (db/id u)
                   :text (str " " arg)}))

(defn do-desc [u arg]
  (try
    (db/set-prop-string! (db/location u) :description arg
                         {:publish true})
    (db/tell u {:text "Description changed."})
    (catch Exception exc
      (info exc)
      (db/tell u {:error (str "Could not set property:" exc)}))))

(defn do-rename [u arg]
  (try
    (let [[id newname] (str/split arg #"=" 2)
          id (Integer/parseInt id)
          dbref (db/db-get id)]
      (if dbref
        (let [oldname (db/name dbref)]
          (db/push-and-publish-changes!
           [[id {:name newname}]])
          (db/tell u {:text (format "Renamed \"%s\" (#%d) => \"%s\""
                                    oldname id newname)}))

        (db/tell u {:text (format "Could not find object with id #%d." id)})))
    (catch Exception exc
      (info exc)
      (db/tell u {:error (str "Could not rename:" exc)}))))

(defn do-dig [u arg]
  (try
    (let [[id room] (db/make-new {:type :room
                                  :name arg})]
      (db/tell u {:text (str "Room \"" arg "\" created with id #" id)}))))

(defn do-create [u arg]
  (try
    (let [[id thing] (db/make-new {:type :thing
                                   :name arg}
                                  :where (db/location u))]
      (db/tell u {:text (str "Thing \"" arg "\" created with id #" id)}))))

(defn do-link [u arg]
  (let [[name dest] (str/split arg #"=" 2)]
    (try
      (let [dest-id (Integer/parseInt dest)]
        (if-let [goto (db/db-get dest-id)]
          (dosync
           (let [[id exit] (db/make-new {:type :exit
                                         :name name
                                         :goto goto}
                                        :where (db/location u))]
             (db/tell u {:text (str "Exit \"" name "\" created with id #" id)})))))

      (catch NumberFormatException nfe
        (db/tell u {:error (str "Invalid number given for dbid: " dest)}))
      (catch Exception exc
        (db/tell u {:error (str exc)})))))

(defn do-delete [u arg]
  (try
    (let [id (Integer/parseInt arg)
          dbref (db/db-get id)]
      (if dbref
        (do
          (db/delete! dbref)
          (db/tell u {:text (str "Deleted \"" (db/name dbref) "\" (#" id ") forever.")}))

        (db/tell u {:error "I don't see that here."})))

    (catch Exception exc
      (db/tell u {:error (str "Could not delete:" exc)})
      (error exc))))

(defn do-spoof [u arg]
  (db/otell-all u {:text (str "* " arg)}))

(defn do-page [u arg]
  (try
    (let [[whom-name msg] (str/split arg #"=" 2)]
      (if-let [whom (db/get-user whom-name)]
        (if (re-find #"^:" msg)
          (do
            (db/tell whom {:text (str "In a page-pose to you, " (db/name u) " " (subs msg 1))})
            (db/tell u {:text (str "You page pose, \"" (db/name u) " " (subs msg 1) "\" to " (db/name whom))}))

          (do
            (db/tell whom {:user (db/id u)
                           :text (str " pages, \"" msg "\"")})
            (db/tell u {:text (str "You page, \"" msg "\" to " (db/name whom) ".")})))

        (db/tell u {:text "I don't see that person."})))
    (catch Exception exc
      (db/tell u {:error (str "Error: " exc)}))))

(defn do-edit [u arg]
  (try
    (let [id (Integer/parseInt arg)]
      (if-let [what (db/db-get id)]
        (db/tell u {:type :edit
                    :edit-id id
                    :dbd (db/db-summary what)})

        (db/tell u {:error (format "I couldn't find an object with that id.")})))

    (catch Exception exc
      (db/tell u {:error (str "Error: " exc)}))))
