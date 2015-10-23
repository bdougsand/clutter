(ns clutter.view.messages
  (:require [goog.dom :as dom]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]

            [clutter.utils :as $]))

(defn message-view
  [message owner]
  (om/component
   (let [{:keys [actor text type stamp]} message]
     (when text
       [:div.message {:class (when type type)}
        (when actor
          [:div.actor actor])
        (when stamp
          [:input {:type "hidden"
                   :name "stamp"
                   :value stamp}])
        (map (fn [x] (cond (string? x) x

                           x (let [href (aget x 0)]
                               [:a {:href href
                                    :target "_blank"} href])))
             ($/match-urls text))]))))

(defn messages-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      [:div#messages
       (om/build-all message-view (:messages app))])

    om/IDidUpdate
    (did-update [_this _prev-props _prev-state]
      (let [elt (om/get-node owner)]
        (when ($/should-scroll? elt)
          (.scrollIntoView (dom/getLastElementChild elt)))))))
