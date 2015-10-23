(ns clutter.view.room
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [sablono.core :as html :refer-macros [html]]))

;; A room is divided into cells.  Each object in the room
(defn cell-view [_ _]
  (reify
      om/IRenderState
    (render-state [_ _]
      [:div.cell])))

(defn room-view [room owner]
  (reify
      om/IRenderState
    (render-state [_ _]
      (html
       [:div.room
        (for [row (range 5)]
          [:div.row
           (for [cell (range 5)]
             [:div.cell])])]))))
