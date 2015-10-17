(ns clutter.filedrop
  (:require [cljs.core.async :as async :refer [<!]]

            [goog.events :as events]

            [clutter.utils :as $])
  (:import [goog.events FileDropHandler]))

(defn process-drop-event
  [evt]
  (let [dtransfer (-> evt (.getBrowserEvent) .-dataTransfer)
        files (array-seq (.-files dtransfer) 0)]
    (assoc
        (if (pos? (count files))
          {:drop :files, :files files}

          (let [text (.getData dtransfer "Text")]
            (when (seq text)
              (cond
               (re-find #"^https?://" text)
               {:drop :url, :url text}

               true
               {:drop :text, :text text}))))
      :event evt)))

(defn make-drag-over-channel
  []
  (let [c (async/chan)
        fd-handler (FileDropHandler. js/document)
        body (.-body js/document)
        handler (fn [evt]
                  (set! (-> evt (.getBrowserEvent)
                            .-dataTransfer
                            .-dropEffect)
                        "copy")
                  ($/cancel evt))]
    (events/listen body "dragover" handler)
    (events/listen body "dragout" handler)
    (events/listen body "drop"
                   (fn [evt]
                     (let [m (process-drop-event evt)]
                       (async/put! c m)
                       (when-not (and ($/input? (.-target evt)) (:text m))
                         ($/cancel evt)))))
    c))

(defonce drag-over-channel (make-drag-over-channel))

;; Tap the filedrop-mult to receive messages whenever a file or URL is
;; dragged over the brower
(defonce filedrop-mult (async/mult drag-over-channel))
