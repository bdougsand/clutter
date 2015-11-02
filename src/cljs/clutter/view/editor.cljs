(ns clutter.view.editor
  (:require [cljsjs.codemirror]

            [goog.dom :as dom]

            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]

            [clutter.editor.editor :as ed]))

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
            (ed/setup cm nil)
            (set! (.-editor js/window) cm))))

      om/IRenderState
      (render-state [_ _]
        (html
         [:div
          [:textarea]
          [:div.docs]]))))
