(ns clutter.editor.reader
  (:require [cljs.reader :as r]

            [clutter.editor.editor :as ed]))

(deftype RBReader [rb ^:mutable li ^:mutable ci ^:mutable line]
  r/PushbackReader
  (read-char [this]
    (when-not line
      (set! line (ed/get-line rb li)))
    (when line
      (let [ll (.-length line)
            ch (if (>= ci ll) "\n" (aget line ci))]
        (if (>= ci (.-length line))
          (do (set! ci 0)
              (set! li (inc li))
              (set! line nil))
          (set! ci (inc ci)))
        ch)))

  (unread [this ch]
    (if (= ci 0)
      (let [nli (dec li)
            nline (ed/get-line rb nli)]
        (when nline
          (set! line nline)
          (set! ci (dec (.-length nline)))
          (set! li nli)))
      (set! ci (dec li)))))

(defn rb-reader
  "Creates a new reader from the given ReadableBuffer."
  [rb]
  (->RBReader rb 0 0 nil))
