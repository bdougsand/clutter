(ns clutter.editor.reader
  (:require [cljs.reader :as r]))

(defn get-range [cm pos]
  (.posFromIndex cm pos))

(deftype CMReader [cm ^:mutable pos]
  r/PushbackReader
  (read-char [this]
    (let [new-pos (inc pos)
          s (.getRange cm (get-range cm pos)
                       (get-range cm new-pos))]
      s))

  (unread [this ch]
    (set! pos (dec pos)))

  ;; r/IndexingReader
  ;; (get-line-number [this]
  ;;   (.-line (get-range cm pos)))

  ;; (get-column-number [this]
  ;;   (.ch (get-range cm pos)))

  ;; (get-file-name [this]
  ;;   (:title this "untitled"))
  )

(defn cm-reader
  "Creates a new reader from the given CodeMirror instance."
  [cm]
  (->CMReader cm 0))
