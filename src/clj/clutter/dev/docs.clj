(ns clutter.dev.docs)

(def namespaces
  #{'clutter.render.canvas
    'clojure.core})

(defn all-vars []
  (vals (apply merge (map (comp ns-publics find-ns) namespaces))))

(defn make-docs []
  (sequence
   (comp (map meta)
         (filter :export)
         (map #(dissoc % :line :column :file :ns)))
   (all-vars)))

(defn add-type [v m]
  (assoc m
         :type (if (not (bound? v))
                 :unbound

                 (let [vv (deref v)]
                   (if (fn? vv) :function (.getSimpleName vv))))))

(defn add-arg-docs [v]
  (assoc v :args (into {} (comp cat
                                (filter meta)
                                (map (fn [s]
                                       [s (meta s)])))
                       (:arglists v))))
