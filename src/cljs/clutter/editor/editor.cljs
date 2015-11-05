(ns clutter.editor.editor
  (:require [cljs.core.async :as async :refer [<! >! chan put!
                                               sliding-buffer
                                               timeout]]
            [cljs.reader :as r]

            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addon.edit.closebrackets]

            [clutter.editor.buffer :as b :refer [as-pos]]
            [clutter.editor.docs :as docs]
            [clutter.editor.search :as s]
            [clutter.editor.reader :as edr])
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]]))

(extend-type js/CodeMirror
  b/ReadableBuffer
  (get-value ([cm] (.getValue cm))
    ([cm sep] (.getValue cm sep)))

  (get-range
      ([cm from to sep] (.getRange cm (as-pos from cm) (as-pos to cm) sep))
      ([cm from to] (b/get-range cm from to "\n")))

  (get-line [cm n] (.getLine cm n))

  (last-line-number [cm] (.lastLine cm))

  (first-line-number [cm] (.firstLine cm))

  b/IndexedBuffer
  (pos-from-index [cm idx]
    (.posFromIndex cm idx))

  b/WritableBuffer
  (set-value [cm value]
    (.setValue cm value))
  (replace-range
      ([cm from to repl]
       (.replaceRange cm repl (as-pos from cm) (as-pos to cm)))
      ([cm from to repl origin]
       ((.replaceRange cm repl (as-pos from cm) (as-pos to cm) origin))))

  b/SelectableBuffer
  (set-selection
      ([cm anchor] (.setSelection cm (as-pos anchor cm)))
      ([cm anchor head] (.setSelection cm (as-pos anchor cm)
                                       (as-pos head cm))))
  (get-cursor
      ([cm] (.getCursor cm))
      ([cm start] (.getCursor cm start)))
  (list-selections [cm]
    (.listSelections cm))

  b/WordBuffer
  (word-range-at [cm pos]
    (.findWordAt cm pos)))


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


(defn setup [cm source doc-fn]
  (let [cursor-in (chan (sliding-buffer 1))
        cursor-chan (wait cursor-in 200)
        docs (docs/default-docs)]
    (.setOption cm "autoCloseBrackets" true)
    (.setOption cm "matchBrackets" true)
    #_
    (when source
      (go-loop []
        (when-let [source-update (<! (update-source cm source))]
          (recur))))

    (when doc-fn
      (go-loop []
        (let [pos (<! cursor-chan)
              form (s/enclosing-form cm (b/get-cursor pos))
              word (b/word-at-pos cm (b/get-cursor pos))]
          (when (list? form)
            (let [doc (<! (docs/docs-for-symbol docs (first form)))]
              (when doc
                (doc-fn doc)
                (js/console.log "Docs:" (prn-str doc))))))
        (recur)))

    (doto cm
      (.on "cursorActivity" #(put! cursor-in %))
      #_
      (.on "changes" (fn [e])))))
