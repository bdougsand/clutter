(ns clutter.editor.editor
  (:require [cljs.core.async :as async :refer [<! >! chan put!
                                               sliding-buffer
                                               timeout]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [cljs.reader :as r]

            [cljsjs.codemirror.mode.clojure]
            #_[cljsjs.codemirror.addon.search.search]

            [clutter.editor.search :as s]
            [clutter.editor.reader :as edr])
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]]))

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

(defn pos-line [r rb]
  (.-line (as-pos r rb)))

(defn pos-ch [r rb]
  (.-ch (as-pos r rb)))

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

(defn end-range [rb]
  (let [ll (last-line-number rb)]))

(defprotocol WritableBuffer
  (set-value [this value])
  (replace-range [this from to repl] [this from to repl origin]))

(defn insert-at [rwb r text]
  (replace-range rwb r r text))

(defn append-text [rwb text]
  (insert-at rwb (end-range rwb) text))


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

(extend-type js/CodeMirror
  ReadableBuffer
  (get-value ([cm] (.getValue cm))
    ([cm sep] (.getValue cm sep)))

  (get-range [cm from to] (.getRange cm (as-pos from cm) (as-pos to cm)))

  (get-line [cm n] (.getLine cm n))

  (last-line-number [cm] (.lastLine cm))

  (first-line-number [cm] (.firstLine cm))

  IndexedBuffer
  (pos-from-index [cm idx]
    (.posFromIndex cm idx))

  WritableBuffer
  (set-value [cm value]
    (.setValue cm value))
  (replace-range
      ([cm from to repl]
       (.replaceRange cm repl (as-pos from cm) (as-pos to cm)))
      ([cm from to repl origin]
       ((.replaceRange cm repl (as-pos from cm) (as-pos to cm) origin))))

  SelectableBuffer
  (set-selection
      ([cm anchor] (.setSelection cm (as-pos anchor cm)))
      ([cm anchor head] (.setSelection cm (as-pos anchor cm)
                                       (as-pos head cm))))
  (get-cursor
      ([cm] (.getCursor cm))
    ([cm start] (.getCursor cm start)))
  (list-selections [cm]
    (.listSelections cm))

  WordBuffer
  (word-range-at [cm pos]
    (.findWordAt cm pos)))

;; (extend-type string
;;   ReadableBuffer
;;   (get-value ([s] s) ([s _] s))
;;   (get-range [s from to] (subs s from to))
;;   (get-line )

;;   WordBuffer
;;   (word-range-at []))


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

(defn wait [c ms]
  (let [out (chan)]
    (go
      (loop [v (<! c)]
        (let [t (timeout ms)]
          (alt! c ([v]
                   (async/close! t)
                   (recur v))
                t ([_]
                   (when (>! out v)
                     (recur (<! c))))))))
    out))


(defn limit-rate [c ms]
  (let [out (chan)]
    (go-loop []
      (let [v (loop [v (<! c), t (timeout ms)]
                (alt! t ([_] v)
                      c ([v] (recur v t))))]
        (>! out v))
      (recur))
    out))

(defn setup [cm source]
  (let [cursor-in (chan (sliding-buffer 1))
        cursor-chan (wait cursor-in 200)]
    #_
    (when source
      (go-loop []
        (when-let [source-update (<! (update-source cm source))]
          (recur))))

    (go-loop []
      (let [pos (<! cursor-chan)
            ;; Position of the previous
            ]

        (js/console.log (get-cursor pos))
        (js/console.log "Word:" (word-at-pos cm (get-cursor pos))))
      (recur))

    (doto cm
      (.on "cursorActivity" #(put! cursor-in %))
      #_
      (.on "changes" (fn [e])))))
