(ns clutter.query
  (:require [clojure.set :as set]))


(defrecord DBref [db id]
  )

(defn dbref? [x]
  #? (:cljs (instance? DBref x)
      :clj (instance? clojure.lang.Ref x)))

(defn deep-merge
  "Painfully simple 'deep merge' of two maps."
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (merge-with deep-merge m1 m2)
    m1))

(defn summary [what]
  {:name (:name what)
   :type (:type what)
   :id (:id what)})

(defn query
  "A query is a description of the data to be retrieved.

  Example:"
  ;; TODO: Potential optimization: Merge the queries rather than the
  ;; results to avoid redundant lookups.
  ([summarize m q]
     (cond
      (integer? q) (some-> (get m q) (summarize))
      (keyword? q) {q (get m q)}
      (vector? q) (reduce deep-merge (map (partial query m) q))
      (map? q) (reduce-kv
                (fn [r k v]
                  (let [q (query (let [x (get m k)]
                                   (if (dbref? x) (deref x) x)) v)]
                    (assoc r k (if-let [old (get r k)]
                                 (deep-merge q old)
                                 q))))
                {} q)))
  ([m q]
   (query summary m q)))

(defn top-level-ids [q]
  (cond
    (integer? q) #{q}
    (keyword? q) nil
    (vector? q) (apply set/union (map top-level-ids q))
    (map? q) (into #{} (filter integer?) (keys q))))
