(ns clutter.view.sidebar
  (:require [cljs.core.async :refer [<! close!]]

            [om.core :as om :include-macros true]

            [sablono.core :as html :refer-macros [html]]

            [clutter.connection :refer [db-get]]
            [clutter.view.brief :refer [brief-view]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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

            (om/build brief-view app {:state {:dbid selection}}))]))))
