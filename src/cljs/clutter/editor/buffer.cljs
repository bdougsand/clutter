(ns clutter.editor.buffer
  (:require [cljs.core.async :as async]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]))

(defprotocol PosLike
  (as-pos [this rb]))

(defprotocol IndexedBuffer
  (pos-from-index [this idx]))

(extend-protocol PosLike
  cljs.core/PersistentVector
  (as-pos [[line ch] _]
    #js {:line line, :ch ch})

  number
  (as-pos [pos rb]
    (pos-from-index rb pos))

  object
  (as-pos [this _] this))

(defn pos-line [p rb]
  (.-line (as-pos p rb)))

(defn pos-ch [p rb]
  (.-ch (as-pos p rb)))

(defn dec-pos [p rb]
  (if (= (.-ch p) 0)
    #js {:ch (.-length (get-line rb (dec (.-line p))))
         :line (dec (.-line p))}
    #js {:ch (dec (.-ch p))
         :line (.-line p)}))

(defprotocol RangeLike
  (anchor [r])
  (head [r]))

(extend-protocol RangeLike
  object
  (anchor [r] (.-anchor r))
  (head [r] (.-head r)))

(defprotocol ReadableBuffer
  (get-value [this] [this sep])
  (get-range [this from to] [this from to sep])
  (get-line [this n])
  (last-line-number [this])
  (first-line-number [this]))

(defn first-line [rb]
  (get-line rb (first-line-number rb)))

(defn last-line [rb]
  (get-line rb (last-line-number rb)))

(defn end-pos
  "Get a position representing the end of the buffer."
  [rb]
  (let [ll (last-line-number rb)]
    #js {:line ll
         :ch (count (get-line rb ll))}))

(defn get-range-from
  ([rb from sep]
   (get-range rb from (end-pos rb)))
  ([rb from]
   (get-range-from rb from "\n")))

(defprotocol WritableBuffer
  (set-value [this value])
  (replace-range [this from to repl] [this from to repl origin]))

(defn insert-at [rwb r text]
  (replace-range rwb r r text))

(defn append-text [rwb text]
  (insert-at rwb (end-pos rwb) text))


(defprotocol SelectableBuffer
  (set-selection [this anchor] [this anchor head])
  (get-cursor [this] [this start])
  (list-selections [this]))

(defprotocol WordBuffer
  (word-range-at [this pos]))

(defn word-range-at-cursor [wb]
  (word-range-at wb (get-cursor wb)))

(defn word-at [wb wr]
  (get-range wb (anchor wr) (head wr)))

(defn word-at-pos [wb pos]
  (let [wr (word-range-at wb pos)]
    (get-range wb (anchor wr) (head wr))))

(defprotocol AsyncDataSource
  (load-data [this])
  (close! [this]))

(extend-protocol AsyncDataSource
  ManyToManyChannel
  (load-data [this] this)
  (close! [this] (async/close! this)))

(defprotocol Update
  (update-source [this cm]))

(defrecord AppendContent [s]
  Update
  (update-source [this wb]
    (append-text wb s)))

(extend-protocol Update
  string
  (update-source [s wb]
    (set-value wb s)))
