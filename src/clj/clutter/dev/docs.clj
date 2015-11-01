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

(defn add-arg-docs [v]
  (into {} (comp cat
                 (filter meta)
                 (map (fn [s]
                        [s (meta s)])))
        (:arglists v)))
