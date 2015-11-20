(ns clutter.view.brief
  (:require [cljs.core.async :refer [<! close!]]

            [om.core :as om :include-macros true]

            [sablono.core :as html :refer-macros [html]]

            [clutter.connection :refer [db-get]]
            [clutter.db :as db]
            [clutter.render.canvas :as canvas])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn brief-multi-view
  "View for multiple selection."
  []
  (reify
      om/IWillMount
    (will-mount [_])

    om/IWillUnmount
    (will-unmount [_])

    om/IRenderState
    (render-state [_ {:keys []}]
      (html
       [:div]))))

(defn draw-view
  [app owner]
  (reify
      om/IWillMount
    (will-mount [_]
      (om/set-state! owner :in (db-get (om/get-state owner :dbid))))

    om/IDidMount
    (did-mount [_]
      (let [node (om/get-node owner)
            in (om/get-state owner :in)]
        (go-loop []
          (when-let [what (<! in)]
            ;; Redraw:
            ))))

    om/IWillUnmount
    (will-unmount [_])

    om/IRenderState
    (render-state [_ _]
      (html
       [:canvas {:width 100
                 :height 100}]))))

;; TODO: Macro or wrapper function to get rid of async get boilerplate.
(defn name-view [app owner]
  (reify
      om/IWillMount
      (will-mount [_]
        (let [in (db-get (om/get-state owner :dbid))]
          (om/set-state! owner :in in)
          (go-loop []
            (when-let [what (<! in)]
              (om/set-state! owner :what what)
              (recur)))))

      om/IWillUnmount
      (will-unmount [_]
        (some-> (om/get-state owner :in) (close!)))

      om/IRenderState
      (render-state [_ {:keys [what on-click]}]
        (html
         [:a.object-name {:class (str (:type what))
                          :href "#"
                          :onClick (when on-click
                                     (fn [e]
                                       (on-click what (:id what) e)))}
          (:name what)]))))

(defn build-name [app id]
  (om/build name-view app
            {:react-key id,
             :state {:dbid id
                     :on-click (fn [what _ _]
                                 (om/update! app :selection #{(:id what)})
                                 (println "Clicked:" (:id what)))}}))

(defn brief-view
  "Renders the description of a selected object."
  [app owner]
  (reify
      om/IWillMount
      (will-mount [_]
        (let [in (db-get (om/get-state owner :dbid))]
          (om/set-state! owner :in in)
          (go-loop []
            (when-let [what (<! in)]
              (om/set-state! owner :what what)
              (recur)))))

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
             (map (partial build-name app)
                  (:contents what))])]))))
