(ns clutter.changes
  "An API for describing changes to a map and then applying them. Shared
  by the server and client."
  (:require [clojure.set :as set]))

#_
(defn verify-change
  "Performs a (shallow) verification of the change."
  [change]
  (every?
   (fn [[k v]]
     (contains? #{:$add :$rem}))))

(defn apply-change
  "Apply a change to the current value v."
  [v change]
  (if (nil? change)
    v

    (let [add (:$add change)
          rem (:$rem change)]
      (if (and rem (map? v))
        (apply dissoc v rem)

        (if (or add rem)
          (set
           (cond-> v
             add (concat add)
             rem (as-> v (remove (set rem) v))))

          (if (map? change)
            (persistent!
             (reduce-kv (fn [m k ch] (assoc! m k (apply-change (get v k) ch)))
                        (transient (or v {})) change))

            change))))))

(defn merge-changes
  [ch1 ch2]
  (cond
   (nil? ch2)
   ch1

   (nil? ch1)
   ch2

   :else
   (let [add1 (some-> (:$add ch1) set)
         add2 (some-> (:$add ch2) set)
         rem1 (some-> (:$rem ch1) set)
         rem2 (some-> (:$rem ch2) set)]
     (if (or add1 add2 rem1 rem2)
       (cond-> ch1
               (or add1 add2) (assoc :$add
                                (set/difference
                                 (set/union add1 add2)
                                 rem2))
               (or rem1 rem2) (assoc :$rem
                                (set/union (set/difference rem1 add2) rem2)))

       (if (map? ch1)
         (if (map? ch2)
           ;; Both changes are represented as maps:
           (merge-with merge-changes ch1 ch2)

           ;; The first change is a map, but the second is something
           ;; else. Ignore the first change.
           ch2)

         ch2)))))
