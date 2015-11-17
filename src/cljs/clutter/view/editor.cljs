(ns clutter.view.editor
  (:require [cljsjs.codemirror]
            [clojure.string :as str]
            [clutter.editor.buffer :as b]
            [clutter.editor.editor :as ed]
            [clutter.utils :as $]
            [goog.dom :as dom]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

;; Currently displayed documentation:
(defonce docs (atom {} ))

(defn arg-str [x]
  (cond
    (string? x) x
    (coll? x) (str "[" (str/join " " (map arg-str x)) "]")))

(defn docs-view [docs owner]
  (om/component
   (html
    (when-let [doc (:doc docs)]
      [:div.docs
       [:div.name (:name doc)]
       [:div.arglists
        (for [alist (:arglists doc)]
          [:div.arglist (arg-str alist)])]
       [:div.docs (:doc doc)]]))))

(defonce editor-value (atom ""))

(defn editor-view [app owner]
  (reify
      om/IDidMount
      (did-mount [_]
        (let [elt (dom/getFirstElementChild (om/get-node owner))
              cm (ed/setup elt nil
                           (fn [d]
                             (swap! docs assoc :doc d)))]
          ;; For debugging:
          (set! (.-editor js/window) cm)))

      om/IRenderState
      (render-state [_ {:keys [value]}]
        (html
         [:div
          [:textarea
           {:value (or value "")
            :onChange (fn [e]
                        (println "saving value")
                        (om/set-state! owner :value
                                       (.. e -target -value)))}]]))))

(defn init []
  (om/root docs-view docs
           {:target ($/by-id "documentation")}))

(init)
