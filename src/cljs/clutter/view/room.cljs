(ns clutter.view.room
  (:require [cljs.core.async :refer [<!]]
            [clutter.connection :refer [db-get]]
            [clutter.db :as db]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [sablono.core :as html :refer-macros [html]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; A room is divided into cells.  Each object in the room
(defn cell-view [_ _]
  (reify
      om/IRenderState
    (render-state [_ _]
      [:div.cell])))

;; Render every object in the room.

(defn room-view [room owner]
  (reify
      om/IDidMount
    (did-mount [_]
      (js/console.log "Mounted"))

      om/IRenderState
    (render-state [_ _]
      (html
       [:div.room
        (for [row (range 5)]
          [:div.row
           (for [cell (range 5)]
             [:div.cell])])]))))
