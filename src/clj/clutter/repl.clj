(ns clutter.repl
  (:require [org.httpkit.server :refer [run-server]]

            [environ.core :refer [env]]

            [clutter.core]
            [clutter.db :as db]
            [clutter.store :as store]))

(def stop-server-fn (atom nil))

(defn stop-server
  []
  (swap! stop-server-fn (fn [f] (when f (f)))))

(defn start-server
  ([port]
   (swap! stop-server-fn
          (fn [f]
            (when f (f))
            (run-server #'clutter.core/app {:port port}))))
  ([]
   (start-server (:port env))))

(defn start-with-db
  ([in port]
   (store/load-db! in)
   (start-server port))
  ([in]
   (start-with-db in (:port env))))

(defn start-with-default
  ([port]
   (store/load-default-db!)
   (start-server port))
  ([]
   (start-with-default (:port env))))
