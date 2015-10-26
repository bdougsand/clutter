(ns clutter.dev.docs)

(def namespaces
  #{'clutter.render.canvas})

(defn all-vars []
  (vals (apply merge (map (comp ns-publics find-ns) namespaces))))

(defn make-docs []
  (sequence
   (comp (map meta)
         (filter :export)
         (map #(dissoc % :line :column :file :ns)))
   (all-vars)))
