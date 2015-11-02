(ns clutter.editor.search
  (:require [clojure.string :as str]
            [clutter.editor.editor :as ed]))

(extend-type cljs.core/PersistentVector
  ed/IndexedBuffer
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

  ed/ReadableBuffer
  (get-value
      ([v] (str/join "\n" v))
      ([v sep]
       (str/join sep v)))
  (get-range
      ([v from to sep]
       (let [ls (ed/pos-line from v)
             le (ed/pos-line to v)
             cs (ed/pos-ch from v)
             ce (ed/pos-ch to v)]
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
       (ed/get-range v from to "\n")))
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
    (let [ll (ed/last-line-number rb)]
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

            (when-let [l (ed/get-line rb li)]
              (recur li (dec (.-length l)) l))))))))

(defn rfind-seq
  ([rf]
   (take-while identity (repeatedly #(.find-prev rf))))
  ([rb pos ch]
   (rfind-seq (->ReverseFind rb #(= % ch) nil
                             (ed/pos-line pos rb)
                             (ed/pos-ch pos rb)))))

(defn rfind-char [rb pos ch]
  (let [rf (->ReverseFind rb #(= % ch)
                          nil
                          (ed/pos-line pos rb)
                          (ed/pos-ch pos rb))]
    (.find-prev rf)))
