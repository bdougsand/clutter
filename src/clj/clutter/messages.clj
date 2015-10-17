(ns clutter.messages
  (:require [clutter.db :as db]))

(defn disconnected
  [who]
  (let [who-id (db/id who)]
    {:db {who-id {:online false}}
     :user who-id
     :text " has disconnected."}))

(defn connected
  [who]
  (let [who-id (db/id who)]
    {:db {who-id {:online true}}
     :user who-id
     :text " has connected."}))

(defn departed
  "Call after who has left loc. Caller should send the resulting message
  to subscribers of loc."
  [who loc]
  {:db {(db/id loc) {:contents (map db/id (db/contents loc))}}
   :text " has left."})

(defn arrived
  "Call after who has arrived in loc."
  [who loc]
  {:db {(db/id loc) {:contents (map db/id )}}}
  (let [loc (db/location who)]))
