(ns clutter.query)

(defn query [m q]
  (cond
   (or (integer? q) (keyword? q)) {q (get m q)}
   (vector? q) (into {} (map (partial query m) q))
   (map? q) (persistent!
             (reduce-kv
              (fn [r k v]
                (assoc! r k (query (get m k) v)))
              (transient {}) q))))
