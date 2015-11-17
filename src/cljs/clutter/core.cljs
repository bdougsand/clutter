(ns ^:figwheel-always clutter.core
  (:require [cljs.core.async :refer [>! <! chan dropping-buffer
                                     mult put! tap] :as async]
            [clojure.set :as set]
            [clojure.string :as str]
            [clutter.connection :as connection :refer [app-state send-message!]]
            [clutter.utils :as $]
            [clutter.view.brief :refer [brief-view]]
            [clutter.view.editor :refer [docs-view editor-view]]
            [clutter.view.room :refer [room-view]]
            [clutter.view.sidebar :refer [sidebar-view]]
            [figwheel.client :as fw]
            [goog.events.KeyCodes :as keycode]
            [goog.events :as events]
            [goog.string :as string]
            [goog.style :as style]
            [om.core :as om :include-macros true]

            [cljsjs.codemirror.mode.clojure])
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



(defn main []
  ;; Mount views:
  #_
  (om/root editor-view app-state
           {:target ($/by-id "editor")})
  (om/root room-view app-state
           {:target ($/by-id "loc_mount")})
  (om/root sidebar-view app-state
           {:target ($/by-id "sidebar")}))

(defn on-js-reload []
  (println "Reloaded")
  (main))

(defn init []
  (connection/init)
  (setup-input!)
  (main))
