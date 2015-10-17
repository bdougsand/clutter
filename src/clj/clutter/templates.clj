(ns clutter.templates
  (:require [net.cgrand.enlive-html :as html :refer [deftemplate]]))

(deftemplate login-page "templates/login.html"
  [{:keys [username login_failed]}]
  [:#username] (html/set-attr :value username)
  [:#flash] (html/content (when login_failed
                            "Incorrect password"))
  [:form] (html/add-class (when login_failed "failed")))
