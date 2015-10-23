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

            [figwheel.client :as fw]

            [clutter.view.brief :refer [brief-view]]
            [clutter.view.room :refer [room-view]]
            [clutter.view.sidebar :refer [sidebar-view]]

            [clutter.connection :as connection :refer [app-state send-message!]]
            [clutter.utils :as $])
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]]))


(enable-console-print!)

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



(defn init []
  (unlisten!)
  (connection/init)
  (setup-input!)
  (setup-clicks!)

  ;; Mount views:

  (om/root room-view app-state
         {:target ($/by-id "loc_mount")}))

(init)


(fw/start {:on-jsload (fn []
                        (println "Reloaded JavaScript.")
                        (init))
           :websocket-url (str "ws://" (.-hostname js/location) ":3447/figwheel-ws")})
