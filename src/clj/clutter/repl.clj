(ns clutter.repl
  (:require [org.httpkit.server :refer [run-server]]

            [environ.core :refer [env]]

            [clutter.core]
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
   (start-server port)))

(defn start-with-db
  [in]
  (store/load-db! in)
  (start-server port))

(defn start-with-default
  []
  (store/load-default-db!)
  (start-server port))
