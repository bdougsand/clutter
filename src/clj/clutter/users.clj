(ns clutter.users
  (:require [clutter.db :as db]))

(defn set-email! [uref email]
  )

(defn reset-password [uref]
  (let [token (str (java.util.UUID/randomUUID))]
    ))
