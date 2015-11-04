(ns clutter.editor.reader
  (:require [cljs.reader :as r]

            [clutter.editor.buffer :as edb]))

(defprotocol Peek
  (peek-char [this]))

(deftype RBReader [rb ^:mutable li ^:mutable ci ^:mutable line]
  Peek
  (peek-char [this]
    (when-not line
      (set! line (edb/get-line rb li)))
    (when line
      (if (>= ci (.-length line)) "\n" (aget line ci))))

  r/PushbackReader
  (read-char [this]
    (let [ch (peek-char this)]
      (when line
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
      (set! ci (dec ci))))

  edb/PosLike
  (as-pos [this _]
    #js {:line li :ch ci})

  Object
  (getPos [this]
    #js {:line li :ch ci}))

#_
(defn reader
  ([rb]
   (r/push-back-reader (edb/get-value rb)))
  ([rb pos]
   (r/push-back-reader (edb/get-range-from rb pos)))
  ([rb from to]
   (r/push-back-reader (edb/get-range rb from to))))

(defn read-form
  [r]
  (r/read r false :eof false))

(defn reader
  "Creates a new reader from the given ReadableBuffer."
  ([rb]
   (->RBReader rb 0 0 nil))
  ([rb pos]
   (->RBReader rb (edb/pos-line pos rb)
               (edb/pos-ch pos rb)
               nil)))
