(ns clutter.store
  (:require [clutter.db :as db]))

(defn load-default-db!
  []
  (dosync
   (ref-set db/dbtop -1)
   (ref-set db/db {})
   (ref-set db/users {})

   (db/make-new
    {:type :room
     :name "Glittertree"
     :props {:description {:string "A tree filled with sparkles."
                           :publish true}}})))

;; TODO: Flatten for saving
(defn flatten-obj
  [ref]
  (reduce-kv
   (fn [m k v]
     (assoc m k (cond
                 (instance? clojure.lang.Ref v)
                 (db/id v)

                 (instance? clojure.lang.Atom v)
                 (deref v)

                 :else
                 v)))))

(defn rebuild-obj
  [m])

(defn dump
  [f]
  )


(comment
  (load-default-db!)
  )
