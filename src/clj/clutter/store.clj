(ns clutter.store
  (:require [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]
            [clojure.walk :refer [postwalk]]

            [clutter.db :as db]
            [clutter.utils :refer [atom? ref?]]))

(defn load-default-db!
  []
  (dosync
   (ref-set db/dbtop -1)
   (ref-set db/db {})
   (ref-set db/users {})

   (db/make-new
    {:type :room
     :name "Chaos"
     :props {:description {:string "In the beginning the Earth was a formless void, and darkness was over the surface of the deep."
                           :publish true}}})))

;; TODO: Flatten for saving
(defn flatten-obj [ref]
  (postwalk (fn [x]
              (cond
               (ref? x) (db/id x)
               (atom? x) @x
               (map? x) (dissoc x :connections)
               :else x))
            @ref))

(defn flatten-db [db]
  (persistent! (reduce-kv (fn [m k v]
                            (assoc! m k (flatten-obj v)))
                          (transient {})
                          @db)))

(defn dump-db
  [out]
  (spit out (prn-str (flatten-db db/db))))

(defn load-db
  [in]
  (let [source (slurp in)
        flat (edn/read-string source)
        db (persistent!
            (reduce-kv (fn [m id obj]
                         (assoc! m id (ref obj)))
                       (transient {})
                       flat))]
    (dosync
     (doseq [[_ dbref] db]
       (alter dbref (fn [{:keys [location contents goto] :as obj}]
                      (cond-> obj
                              location (assoc :location (db location))
                              contents (assoc :contents (into #{} (map db contents)))
                              goto (assoc :goto (db goto))))))
     db)))

(defn user-map
  "Returns a map of user names (lower case) to dbrefs. dbmap should be a
  map of ids to dbrefs, as returned by load-db."
  [dbmap]
  (persistent!
   (reduce-kv (fn [umap id dbref]
                (if (= (:type @dbref) :user)
                  (assoc! umap (str/lower-case (:name @dbref)) dbref)
                  umap))
              (transient {})
              dbmap)))

(defn load-db!
  [in]
  (let [new-db (load-db in)
        users (user-map new-db)]
    (dosync
     (ref-set db/db new-db)
     (ref-set db/users users))))



(comment
  (load-default-db!)
  )
