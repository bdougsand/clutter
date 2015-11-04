(ns clutter.editor.search
  (:require [clojure.string :as str]
            [cljs.reader :as r]

            [cljsjs.codemirror.addon.edit.closebrackets]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.search.searchcursor]

            [clutter.editor.buffer :as b]
            [clutter.editor.reader :as edr]))

(extend-type cljs.core/PersistentVector
  b/IndexedBuffer
  (pos-from-index [v idx]
    (loop [li 0, idx idx]
      (when-let [line (nth v li)]
        (let [ll (.-length line)]
          (cond (> idx ll)
                (recur (inc li) (- idx ll))

                (= idx ll)
                #js {:line (inc li) :ch 0}

                :default
                #js {:line li, :ch idx})))))

  b/ReadableBuffer
  (get-value
      ([v] (str/join "\n" v))
      ([v sep]
       (str/join sep v)))
  (get-range
      ([v from to sep]
       (let [ls (b/pos-line from v)
             le (b/pos-line to v)
             cs (b/pos-ch from v)
             ce (b/pos-ch to v)]
         (if (= le ls)
           (subs (nth v ls) cs ce)

           (str/join
            sep
            (let [[fl & ls] (sequence (comp (drop ls)
                                            (take (- le ls -1)))
                                      v)]
              (concat (cons (subs fl cs) (butlast ls))
                      [(subs (last ls) 0 ce)]))
            ))))
      ([v from to]
       (b/get-range v from to "\n")))
  (get-line [v i]
    (when (and (>= i 0) (< i (count v)))
      (nth v i)))
  (last-line-number [v]
    (dec (count v)))
  (first-line-number [v]
    0))

(deftype ReverseFind [rb pred
                      ^:mutable line
                      ^:mutable lindex
                      ^:mutable cindex]
  Object
  ;; Find the previous occurrence of a character:
  (find-prev [this]
    (let [ll (b/last-line-number rb)]
      (loop [li lindex, ci cindex, l line]
        (when (and (>= li 0) (<= li ll))
          (if l
            (cond
              (< ci 0) (recur (dec li) -1 nil)

              :default
              (if (pred (aget l ci))
                (do
                  (set! line l)
                  (set! lindex li)
                  (set! cindex ci)
                  #js {:line li, :ch ci})

                (recur li (dec ci) l)))

            (when-let [l (b/get-line rb li)]
              (recur li (dec (.-length l)) l)))))))

  (get-pos [this]
    #js {:line lindex, :ch cindex}))

(defn pair-scanner [r inc-pred dec-pred]
  (loop [n 0, spos nil]
    (let [p (b/as-pos r nil)
          ch (r/read-char r)]
      (when ch
        (cond (inc-pred ch) (recur (inc n) (when (= 0 n) p))

              (dec-pred ch) (if (= n 1) [spos p] (recur (dec n) spos))

              :else (recur n spos))))))

(defn parens-scanner [r]
  (pair-scanner r #(= % "(") #(= % ")")))

(defn rfind-seq
  ([rf]
   (take-while identity (repeatedly #(.find-prev rf))))
  ([rb pos ch]
   (rfind-seq (->ReverseFind rb #(= % ch) nil
                             (b/pos-line pos rb)
                             (b/pos-ch pos rb)))))

(defn rfind-char [rb pos ch]
  (let [rf (->ReverseFind rb (if (ifn? ch) ch  #(= % ch))
                          nil
                          (b/pos-line pos rb)
                          (b/pos-ch pos rb))]
    (.find-prev rf)))



(defn get-next [sc]
  (when (.findNext sc) [(.from sc) (.to sc)]))

(defn get-prev [sc]
  (when (.findPrevious sc) [(.from sc) (.to sc)]))

(defn get-matches
  [sc f]
  (when-let [x (f sc)]
    (cons x (lazy-seq (get-matches sc)))))

(defn find-matches
  ([cm query pos ci]
   (get-matches (.getSearchCursor cm query pos ci) get-next)))

#_
(defn enclosing-form
  "Given a position inside the readable buffer, find the enclosing list
  form."
  ([rb pos]
   (when-let [rpos (rfind-char rb pos "(")]
     (edr/read-form (edr/reader rb rpos))))
  ([rb]
   (enclosing-form rb (b/get-cursor rb))))

(defn enclosing-form
  ([cm pos]
   (when-let [to (ffirst (find-matches cm ")" pos false))]
     (js/console.log to)
     (when-let [match (.findMatchingBracket cm to)]
       (js/console.log match)
       (edr/read-form (edr/reader cm (.-to match))))))
  ([cm]
   (enclosing-form cm (b/get-cursor cm))))
