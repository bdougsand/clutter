(ns clutter.view.editor
  (:require [cljsjs.codemirror]

            [goog.dom :as dom]

            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]

            [clutter.editor.buffer :as b]
            [clutter.editor.editor :as ed]

            [clutter.utils :as $]))

(defonce docs (atom {} ))

(defn docs-view [docs owner]
  (om/component
   (html
    (when-let [doc (:doc docs)]
      [:div.docs
       [:div.name (:name doc)]
       [:div.arglists
        (for [alist (:arglists doc)]
          [:div.arglist (prn-str alist)])]
       [:div.docs (:doc doc)]]))))

(defn editor-view [app owner]
  (reify
      om/IDidMount
      (did-mount [_]
        (let [elt (dom/getFirstElementChild (om/get-node owner))]
          ;; Set up CodeMirror instance
          (let [cm (js/CodeMirror.fromTextArea
                    elt
                    #js {:lineNumbers false
                         :mode "clojure"})]
            (ed/setup cm nil (fn [d]
                               (swap! docs assoc :doc d)))
            (set! (.-editor js/window) cm))))

      om/IRenderState
      (render-state [_ _]
        (html
         [:div
          [:textarea]]))))

(defn init []
  (om/root docs-view docs
           {:target ($/by-id "documentation")}))

(init)
