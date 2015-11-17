(ns clutter.dev.docs
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.reader.edn :refer [read read-string]]

            [clutter.dev.names :as names]))

(defn rsplit [s m]
  (let [rind (.lastIndexOf s m)]
    (if (> rind -1)
      [(subs s 0 rind) (subs s (+ rind (count m)))]
      [s])))

(defn exported-namespaces
  "Takes a map of symbol => {:name ns-qualified-symbol}, returns a
  collection of unique namespaces exported."
  [names]
  (set (sequence (comp (map (comp :name val))
                       (map (fn [qname]
                              (symbol (first (rsplit (str qname) "."))))))
                 names)))

(defn file-forms [path]
  (let [r (java.io.PushbackReader. (io/reader path) 1024)]
    (take-while #(not= % :eof)
                (repeatedly #(read r false :eof
                                   {:default (fn [t v] v)})))))

(defn meta-map [forms]
  (into {} (comp
            (filter #(re-find #"^defn(?!-)" (name (first %))))
            (map (fn [[_ form]]
                   [form (meta form)])))
        forms))

(defn file-map
  "Retrieves a map of symbol => metadata."
  [path]
  (meta-map (file-forms path)))

;; Replace this with something less fragile:
(defn cljs-file [ns-str]
  (str "resources/public/js/out/" (str/replace ns-str "." "/") ".cljs"))

#_
(defn publics [state ns-sym]
  (sequence
   (comp (map (fn [[k v]]
                (assoc (:meta v)
                       :basename k
                       ))))
   (ns/publics state ns-sym)))

(def file-vars
  "Takes a namespace string. Returns a map of top level symbols =>
  metadata maps."
  (comp file-map cljs-file))

(defn all-meta [names]
  (let [nss (exported-namespaces names)
        ns-maps (apply merge (file-vars nss))]
    ))

(def blacklist
  '#{})

(def namespaces
  #{'clutter.render.canvas
    })

(defn all-vars []
  (doseq [nss namespaces] (require nss))
  (vals (apply merge (map (comp ns-publics find-ns) namespaces))))

(defn make-docs []
  (into {}
        (comp (map meta)
              (filter (fn [m]
                        (or (:export m) (not (blacklist (:name m))))))
              (map #(dissoc % :line :column :file :ns))
              (map (juxt :name identity)))
        (all-vars)))

(defn add-type [v m]
  (assoc m
         :type (if (not (bound? v))
                 :unbound

                 (let [vv (deref v)]
                   (if (fn? vv) :function (.getSimpleName vv))))))

(defn add-arg-docs [v]
  (assoc v :args (into {}
                       (comp cat
                             (filter meta)
                             (map (fn [s]
                                    [s (meta s)])))
                       (:arglists v))))

(defn symbol-info [s]
  )
