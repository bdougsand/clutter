(ns clutter.view.brief
  (:require [cljs.core.async :refer [<! close!]]

            [om.core :as om :include-macros true]

            [sablono.core :as html :refer-macros [html]]

            [clutter.connection :refer [db-get query]]
            [clutter.db :as db])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn brief-multi-view [])

;; TODO: Macro or wrapper function to get rid of async get boilerplate.
(defn name-view [_ owner]
  (reify
      om/IWillMount
      (will-mount [_]
        (let [in (db-get (om/get-state owner :dbid))]
          (om/set-state! owner :in in)
          (go-loop []
            (om/set-state! owner :what (<! in))
            (recur))))

      om/IWillUnmount
      (will-unmount [_]
        (some-> (om/get-state owner :in) (close!)))

      om/IRenderState
      (render-state [_ {:keys [what]}]
        [:div.object-name {:class (str (:type what))}
         (:name what)])))

(defn build-name [id]
  (om/build name-view what {:react-key id,
                            :dbid id}))

(defn brief-view
  "Renders the description of a selected object."
  [_ owner]
  (reify
      om/IWillMount
      (will-mount [_]
        (let [in (db-get (om/get-state owner :dbid))]
          (om/set-state! owner :in in)
          (go-loop []
            (om/set-state! owner :what (<! in))
            (recur))))

      om/IWillUnmount
      (will-unmount [this]
        (some-> (om/get-state owner :in) (close!)))

      om/IRenderState
      (render-state [_ {:keys [what]}]
        (html
         [:div.brief
          [:div.description
           (db/get-prop-string what :description)]
          (when (:contents what)
            [:div.contents
             (map build-name (map (partial db/get-object )))
             (map build-name (:contents what))])]))))
