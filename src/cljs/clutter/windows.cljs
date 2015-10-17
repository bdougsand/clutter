(ns clutter.windows
  (:require [cljs.core.async :as async :refer [>! <! chan]]

            [goog.dom :as dom]
            [goog.events :as events]
            [goog.style :as style]

            [clutter.events :as e]
            [clutter.utils :as $])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))



(defn popup
  [key title content]
  )
