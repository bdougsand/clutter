(ns clutter.db
  (:require [cljs.core.async :refer [>! <! chan close!
                                     dropping-buffer mult put! tap]
             :as async]

            [clutter.connection :as conn]
            [clutter.query :as q]))

(defonce query-subscriptions (atom {}))

;; Implement:
(defn add-subscription [subscriptions q]
  )


(defn get-user [{:keys [db-cache user-id]}]
  (get db-cache user-id))

(defn get-object [{:keys [db-cache]} id]
  (get db-cache id))

(defn get-location [{:keys [db-cache]}])

(defn get-prop-string [what p]
  (some-> what :props p :string))

(defn get-contents [what])
