(ns clutter.editor.search
  (:require [clojure.string :as str]
            [clutter.editor.editor :as ed]))

(extend-protocol ed/ReadableBuffer
  cljs.core/PersistentVector
  (get-value
      ([v] (str/join "\n" v))
      ([v sep]
       (str/join sep v)))
  (get-range
      ([v from to sep]
       (let [ls (ed/range-line from v)
             le (ed/range-line to v)
             cs (ed/range-ch from v)
             ce (ed/range-ch to v)]
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
    (nth v i))
  (last-line-number [v]
    (dec (count v)))
  (first-line-number [v]
    0))

(deftype ReverseFind [rb ^:mutable line
                      ^:mutable lindex
                      ^:mutable cindex]
  Object
  ;; Find the previous occurrence of a character:
  (find-prev [this pred]
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

(defn rfind [rf]
  )

(defn rfind-char [rb pos ch]
  )
