(ns clutter.editor.reader
  (:require [cljs.reader :as r]

            [clutter.editor.buffer :as edb]))

(deftype RBReader [rb ^:mutable li ^:mutable ci ^:mutable line]
  r/PushbackReader
  (read-char [this]
    (when-not line
      (set! line (edb/get-line rb li)))
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
            nline (edb/get-line rb nli)]
        (when nline
          (set! line nline)
          (set! ci (.-length nline))
          (set! li nli)))
      (set! ci (dec ci)))))

(defn reader
  ([rb]
   (r/push-back-reader (edb/get-value rb)))
  ([rb pos]
   (r/push-back-reader (edb/get-range-from rb pos))))

(defn read-form
  [r]
  (r/read r false :eof false))

#_
(defn reader
  "Creates a new reader from the given ReadableBuffer."
  ([rb]
   (->RBReader rb 0 0 nil))
  ([rb pos]
   (->RBReader rb (edb/pos-line pos rb)
               (edb/pos-ch pos rb)
               nil)))
