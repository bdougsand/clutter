(ns clutter.view.sidebar
  (:require [cljs.core.async :refer [<! close!]]

            [om.core :as om :include-macros true]

            [sablono.core :as html :refer-macros [html]]

            [clutter.connection :refer [db-get]]
            [clutter.view.brief :refer [brief-view]]
            [clutter.render.canvas :as canvas])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn render-view [app owner]
  (reify
      om/IDidMount
    (did-mount [_]
      (let [elt (om/get-node owner)]
        (canvas/render-canvas
         [(->> (canvas/circle 50 50 40)
                (canvas/stroke :red)
                (canvas/line-width 5))
          (canvas/stroke :green
                         (canvas/circle 25))]
         elt)))

    om/IRender
    (render [_]
      (html [:canvas]))))

(defn sidebar-view [app owner]
  (reify
      om/IWillMount
      (will-mount [_]
        (let [in (db-get (:user-id app))]
          (om/set-state! owner :in in)
          (go-loop []
            (when-let [what (<! in)]
              (om/set-state! owner :what what)
              (recur)))))

      om/IWillUnmount
      (will-unmount [_]
        (some-> (om/get-state owner :in) (close!)))

      om/IRenderState
      (render-state [_ {:keys [what]}]
        (html
         [:div.sidebar
          (when-let [selection (some-> what :location)]

            (om/build brief-view app {:state {:dbid selection}}))

          (om/build render-view nil)]))))
