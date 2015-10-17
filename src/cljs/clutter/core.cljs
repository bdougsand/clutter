(ns clutter.core
  (:require [cljs.core.async :refer [>! <! chan dropping-buffer
                                     mult put! tap] :as async]
            [clojure.set :as set]
            [clojure.string :as str]

            [goog.date.relative :as rel]
            [goog.dom :as dom]
            [goog.dom.classes :as classes]
            [goog.events :as events]
            [goog.events.KeyCodes :as keycode]
            [goog.string :as string]
            [goog.style :as style]

            [om.core :as om :include-macros true]
            [om.dom :as om-dom :include-macros true]

            [chord.client :refer [ws-ch]]

            [figwheel.client :as fw]

            [clutter.changes :refer [apply-change merge-changes]]
            [clutter.renderable :as r :refer [escaped]]
            [clutter.utils :as $])
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]]))


(enable-console-print!)

(defonce connection
  (atom nil))

(defonce app-state
  (atom {:db-cache {}
         :user-id nil
         :messages []
         :editing #{}}))

(defonce query-chan (chan))
(defonce query-mult (mult query-chan))

(defn db-get
  [id]
  (get-in @app-state [:db-cache id]))

(defn db-query [id]
  (when (put! @connection {:dbq id})
    (go
     (let [c (tap query-mult (chan))]

       (loop []
         (let [m (<! c)]
           (if-let [val (get m id)]
             (do
               (async/close! c)
               val)

             (recur))))))))

(defn send-message!
  [msg]
  (when-let [conn @connection]
    (prn "Sending message:" msg)
    (async/put! conn msg)))


;; The request loop buffers database queries.
(defonce request-chan (async/chan))

(defn start-db-request-loop
  []
  (go-loop []
    (let [queries (go-loop [queries (list (<! request-chan))
                            timer nil]
                    (let [timer (or timer (async/timeout 50))]
                      (alt! request-chan ([q] (recur (cons q queries) timer))
                            timer queries)))]
      ;; Send the consolidated query to the server
      (send-message! {:dbq queries})
      (recur))))

(defn connect!
  [handler]
  (go
    (let [url (str "ws://" (.-host js/location) "/connect")
          {:keys [ws-channel error]} (<! (ws-ch url))]
      (if-not error
        (do
          (reset! connection ws-channel)
          (loop []
            (let [{m :message, err :error} (<! ws-channel)]
              (when-not err
                (handler m)
                (recur)))))

        (prn "Connection error:" error)))))

(defn ids->objects
  ([db ids]
   (persistent!
    (reduce (fn [v id]
              (conj! v (get db id)))
            (transient [])
            ids))))

(defn eval-prop [prop & [default]]
  (cond
   (string? prop) prop

   (:code prop) (try
                  (or ((js/eval (:code prop))) default)
                  (catch js/Error err
                    (str "Error while evaluating code:\n"
                         err
                         "\n\n"
                         (:string prop))))

   (:string prop) (:string prop)

   (not prop) default))

;; OM Components:
(defn summary-view
  ""
  [thing owner]
  (reify
    om/IRender
    (render [this]
      (om-dom/span #js {:className
                        (str/join " " ["summary"
                                       (name (:type thing))
                                       (when (contains? thing :online)
                                         (if (:online thing)
                                           "online" "offline"))])}
                   (:name thing)))))

(defn exit-view
  "Render a single exit."
  [exit owner]
  (om/component
   (om-dom/div #js {:className "exit"}
               (om-dom/a #js {:href "#"
                              :className "_clickable _click[exit]"}
                         (om-dom/input
                          #js {:type "hidden"
                               :name "dbid"
                               :value (:id exit)})
                         (:name exit))
               (str " (#" (:id exit) ")"))))

(defn thing-view
  [thing owner]
  (om/component
   (om-dom/div #js {:className "thing"}
               (om-dom/input #js {:type "hidden"
                                  :name "dbid"
                                  :value (:id thing)})
               (om-dom/span #js {:className "name"} (:name thing))
               (str " (#" (:id thing) ")"))))

(defn location-view
  [app owner]
  (om/component
   (let [db (:db-cache app)
         uid (get app :user-id)
         user (get db uid)
         loc (get db (:location user))
         contents (ids->objects db (:contents loc))
         con-groups (group-by :type contents)]
     (om-dom/div #js {:id "location"}
                 (om-dom/h3 #js {:id "loc_name"} (:name loc) (str " (#" (:id loc) ")"))
                 (om-dom/div #js {:id "loc_desc"}
                             (eval-prop
                              (:description (:props loc))
                              "You see nothing special."))
                 (when-let [exits (:exit con-groups)]
                   (apply om-dom/div #js {:id "loc_exits"}
                          (om-dom/strong nil "Exits:")
                          (om/build-all exit-view exits)))

                 (when-let [things (:thing con-groups)]
                   (apply om-dom/div #js {:id "loc_things"}
                          (om-dom/strong nil "Contents:")
                          (om/build-all thing-view things)))

                 (let [users (:user con-groups)]
                   (apply om-dom/div #js {:id "loc_users"
                                          :className "summary_list"}
                          (om-dom/span #js {:className "label"} "Users:")
                          (om/build-all summary-view users)))))))

(defn message-view
  [message owner]
  (om/component
   (let [{:keys [actor text type stamp]} message]
     (when text
       (apply om-dom/div #js {:className (cond-> "message"
                                             type (str " " type))}
              (when actor
                (om-dom/span #js {:className "actor"} actor))
              (when stamp
                (om-dom/input #js {:type "hidden"
                                   :name "stamp"
                                   :value stamp}))
              (map (fn [x]
                     (cond
                      (string? x) x

                      (not (nil? x))
                      (let [href (aget x 0)]
                        (om-dom/a #js {:href href
                                       :target "_blank"} href))))
                   ($/match-urls text)))))))


(defn should-scroll? [elt]
  (let [lc-height (or (some-> (dom/getLastElementChild elt)
                              (style/getSize)
                              .-height)
                      15)]
    (<= ($/scroll-dist-from-bottom elt) (+ lc-height 5))))

(defn messages-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      (apply om-dom/div #js {:id "messages"}
             (om/build-all message-view (:messages app))))

    om/IDidUpdate
    (did-update [_this _prev-props _prev-state]
      (let [elt (om/get-node owner)]
        (when (should-scroll? elt)
          (.scrollIntoView (dom/getLastElementChild elt)))))))

(defn append-message
  [actor text stamp & [type]]
  (swap! app-state
         (fn [{ml :messages :as state}]
           (assoc state :messages (conj ml {:actor actor
                                            :text text
                                            :stamp stamp
                                            :type type})))))

(defmulti handle-type :type)
(defmethod handle-type :default [_] nil)
(defmethod handle-type :edit [m]
  (swap! app-state (fn [st]
                     (assoc st
                       :editing (conj (:editing st) (:edit-id m))))))


(defn handle-message
  [m]
  (prn "Received:" m)
  (let [{t :text, db :db, e :error, dbd :dbd} m
        user (db-get (:user m))
        uname (escaped (:name user ""))]
    (when-let [uid (:user-id m)]
      (swap! app-state assoc :user-id uid))

    (when db
      ;; Update the DB. New values will supersede old.
      (swap! app-state (fn [st]
                         (let [dbc (:db-cache st)]
                           (assoc st
                             :db-cache (merge-with merge dbc db))))))

    (when dbd
      ;; dbd == db-delta
      ;; See changes namespace for documentation.
      (swap! app-state (fn [st]
                         (assoc st
                           :db-cache (apply-change (:db-cache st) dbd))))

      (put! query-chan dbd))

    ;; Any message can have a visible component that is printed
    ;; directly.
    (when (or t e)
      (append-message uname (or e t) (:stamp m (.valueOf (js/Date.))) (when e "error")))

    (handle-type m)))


#_(defmethod render-prop-val :type)

(defn prop-view
  [[pname pval :as prop] owner]
  (reify
    om/IRender
    (render [_]
      (om-dom/div #js {:className "prop"}
                  (om-dom/div #js {:className "propname"}
                              (name pname))
                  (om-dom/span #js {:className "propval"}
                               (:string pval))))))

(defn prop-editor
  [db-object owner]
  (reify
    om/IInitState
    (init-state [_]
      {:dragger (chan (dropping-buffer 1))
       :dragging false
       :x 0
       :y 0})

    om/IWillMount
    (will-mount [_]
      (let [start (om/get-state owner :dragger)]
        (go-loop []
          (let [{:keys [dx dy]} (<! start)
                move (tap (om/get-shared owner :move) (chan (dropping-buffer 1)))]
            (om/set-state! owner :dragging true)
            (loop []
              (let [m (<! move)]
                (when (= (:type m) :move)
                  (om/update-state! owner
                                    (fn [st]
                                      (assoc st
                                        :x (- (:x m) dx)
                                        :y (- (:y m) dy))))
                  (recur))))
            (om/set-state! owner :dragging false))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [dragger dragging x y]}]
      (om-dom/div #js {:className (str "prop_popup" (when dragging " dragging"))
                       :style #js {:left (str x "px")
                                   :top (str y "px")}}
                  (om-dom/div
                   #js {:className "popup_title"
                        :onMouseDown (fn [e]
                                       (when (put! dragger
                                                   {:dx (- (.-screenX e) x)
                                                    :dy (- (.-screenY e) y)})
                                         ($/cancel e)))}
                   (str "Editing: " (:name db-object) " (#" (:id db-object) ")")
                   (om-dom/a #js {:className "popup_close"
                                  :href "#"
                                  :onClick (fn [e]
                                             (put! (om/get-shared owner :close) (:id db-object))
                                             ($/cancel e))
                                  :dangerouslySetInnerHTML #js {:__html "&times;"}}))
                  (apply om-dom/div #js {:className "prop_editor"}
                         (om/build-all prop-view (:props db-object)))))))

(defn prop-editors-view
  [app-state owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [close (om/get-shared owner :close)]
        (go-loop []
          (let [id (<! close)]
            (om/transact! app-state :editing
                          (fn [ids] (disj ids id))))
          (recur))))

    om/IRender
    (render [_]
      (apply om-dom/div nil (map (fn [id]
                                   (om/build prop-editor (get-in app-state [:db-cache id])))
                                 (:editing app-state))))))

(defonce listener-keys (atom nil))
(defonce command-history (atom {:position 0
                                :input  []}))


(defn unlisten!
  []
  (doseq [k @listener-keys]
    (events/unlistenByKey k))
  (reset! listener-keys nil))

(defn setup-input!
  []
  (when-let [input ($/by-id "input")]
    (swap! listener-keys conj
           (events/listen input "keypress"
                          (fn [e]
                            (let [input (.-target e)]
                              (when (and (= (.-keyCode e) keycode/ENTER)
                                         (not (.-shiftKey e)))
                                (when (send-message! {:type :message
                                                      :text (.-value input)})
                                  (set! (.-value input) "")
                                  (.preventDefault e)))))))))

(defn choose-nick
  []
  (loop []
    (let [n (js/prompt "Please choose a nickname (at least 3 characters long):")]
      (if (< (.-length n) 3)
        (recur)
        n))))

(def click-type-handler
  {:exit (fn [link e]
           (let [what-id (try
                           (js/parseInt ($/get-named-val link "dbid"))
                           (catch js/Error _ nil))]
             (send-message! {:type :use
                             :what what-id})
             (.preventDefault e)
             false))})

(def click-matcher #"_click\[(\w+)\]")

(defn setup-clicks!
  []
  (swap! listener-keys conj
         (events/listen js/document "click"
                        (fn [e]
                          (when-let [target (dom/getAncestorByClass (.-target e) "_clickable")]
                            (when-let [[_ click-type] (re-find click-matcher (.-className target))]
                              (prn "Processing click type:" click-type)
                              (when-let [handler (click-type-handler (keyword click-type))]
                                (handler target e))))))))

(defn init []
  (unlisten!)
  (when-not @connection
    (connect! (fn [m] (handle-message m))))
  (setup-input!)
  (setup-clicks!))

(init)

(om/root location-view app-state
         {:target ($/by-id "loc_mount")})
(om/root messages-view app-state
         {:target ($/by-id "output")})


(defonce move-mult
  (let [c (chan)]
    (events/listen js/document "mousemove"
                   #(put! c {:type :move
                             :x (.-screenX %)
                             :y (.-screenY %)}))
    (events/listen js/document "mouseup"
                   (fn [e] (put! c {:type :end})))
    (mult c)))

(om/root prop-editors-view app-state
         {:target ($/by-id "prop_editors")
          :shared {:move move-mult
                   :close (chan)}})

(fw/start {:on-jsload (fn []
                        (println "Reloaded JavaScript.")
                        (init))
           :websocket-url (str "ws://" (.-hostname js/location) ":3447/figwheel-ws")})
