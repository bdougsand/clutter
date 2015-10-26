(ns clutter.test.changes
  (:use [clojure.test])
  (:require [clutter.changes :as changes :refer [apply-change
                                                    merge-changes]]))

(def base-db
  {0 {:type :room
      :name "Chaos"
      :props {:description {:string "A void."
                            :publish true}}}})

(defn create-db [] base-db)

(def john
  {:type :user
   :name "John"
   :props {:description {:string "A shriveled little troll of a man with a heart of gold (not shown)."}}})

(deftest no-change
  (is (= (apply-change base-db nil) base-db)
      "Applying a nil change to the DB should not change the DB."))

(deftest simple-change
  (is (= (apply-change base-db {1 john})
         (assoc base-db 1 john))))

(deftest add-contents
  (is (= (-> (apply-change base-db {0 {:contents {:$add [1]}}})
             (get-in [0 :contents]))
         #{1})
      "Adding contents did not work."))

(deftest remove-key
  (is (= (apply-change base-db {:$rem [0]})
         {})))

(deftest multiple-changes
  (is (= (-> (apply-change base-db
                           {})))))
