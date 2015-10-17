(ns clutter.events
  (:require [cljs.core.async :as async :refer [>! <! chan]]

            [goog.dom :as dom]
            [goog.events :as events]
            [goog.style :as style]

            [clutter.utils :as $])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn drag-watch
  "Returns a new channel that receives messages of the form
  [drag-element x-offset y-offset start]."
  [dragger?]
  (let [c (chan (async/sliding-buffer 1))
        f (fn [e] (async/put! c e))
        out (chan (async/sliding-buffer 1))
        get-dragger (fn [e] (dom/getAncestor (.-target e) dragger? true))]
    (events/listen js/document "mousedown"
                   (fn [e]
                     (when (get-dragger e)
                       ($/cancel e)
                       (f e))))
    (events/listen js/document "mousemove" f)
    (events/listen js/document "mouseup" f)

    (go-loop [state :waiting, drag nil, dx 0, dy 0]
      (let [e (<! c)
            type (.-type e)]
        (case state
          :waiting
          (case (.-type e)
            ("mousedown")
            (when-let [drag (get-dragger e)]
              (let [offset (style/getPageOffset drag)
                    xoff (.-x offset)
                    yoff (.-y offset)]
                (>! out [drag xoff yoff true])
                (recur :holding drag
                       (- xoff (.-pageX e))          ; dx
                       (- yoff (.-pageY e))          ; dy
                       )))

            ;; ignore mouseup/move
            nil)

          :holding
          (case (.-type e)
            ("mousemove")
            (do
              (>! out [drag (+ dx (.-pageX e)) (+ dy (.-pageY e))])
              (recur :holding drag dx dy))

            ("mouseup")
            (recur :waiting nil nil nil))

          (recur state drag dx dy))))

    out))
