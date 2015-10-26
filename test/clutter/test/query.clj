(ns clutter.test.query
  (:require [clojure.test :refer :all]
            [clutter.query :refer :all]
            [clutter.test.changes :refer [base-db]]))

(deftest top-level-ids-test
  (is (= (top-level-ids 1) #{1}))
  (is (= (top-level-ids [0 1 2 3]) #{0 1 2 3}))
  (is (= (top-level-ids [{0 :name} 1 2 3]) #{0 1 2 3}))
  (is (= (top-level-ids {0 :name, 1 :name}) #{0 1})))

(deftest query-test
  (is (= (-> base-db
             (query {0 :name})
             (get 0))
         (select-keys (get base-db 0) [:name]))))


(query {:a 1} [:a :b])
