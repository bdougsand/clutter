(ns clutter.editor.editor
  (:require [cljs.core.async :as async :refer [<! >! chan put!
                                               sliding-buffer
                                               timeout]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]

            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addon.search.search])
  (:require-macros [cljs.core.async.macros :refer [alt! go-loop]]))

(defprotocol PosLike
  (as-pos [this rb]))

(extend-protocol PosLike
  vector
  (as-pos [[line ch] _]
    #js {:line line, :ch ch})

  number
  (as-pos [pos rb]
    (.posFromIndex rb pos))

  object
  (as-pos [this _] this))

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

(defn limit-rate [c ms]
  (let [out (chan)]
    (go-loop [v nil, t (timeout ms)]
      (alt! t ([_]
               (when v (>! out v))
               (recur nil (timeout ms)))
            c ([v]
               (recur v t))))
    out))

(defn setup [cm source]
  (let [cursor-chan (limit-rate
                     (chan (sliding-buffer 1) (map get-cursor))
                     200)]
    (when source
      (go-loop []
        (when-let [source-update (<! (update-source cm source))]
          (recur))))

    (go-loop []
      (let [pos (<! cursor-chan)]
        (js/console.log "Got new cursor position:" (word-at-pos cm (get-cursor pos))))
      (recur))

    (doto cm
      (.on "cursorActivity" (fn [pos]
                              (put! cursor-chan pos)))
      #_
      (.on "changes" (fn [e])))))
