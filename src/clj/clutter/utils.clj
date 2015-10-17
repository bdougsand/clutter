(ns clutter.utils
  (:require [clojure.stacktrace :refer [print-stack-trace]]

            [ring.util.response :as response]

            [taoensso.timbre :as timbre :refer [info warn error]]))

(defn commas
  [l]
  (let [bl (butlast l)]
    (str
     (apply str (interpose ", " bl))
     (when bl ", and ")
     (last l))))

(defn ref? [x]
  (instance? clojure.lang.Ref x))

(defn atom? [x]
  (instance? clojure.lang.Atom x))

(defn wrap-exceptions
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception exc
        (let [err-s (with-out-str
                      (print-stack-trace exc))]
          (error err-s)
          (-> (response/response
               err-s)
              (response/status 500)))))))

(defn wrap-log
  [handler]
  (fn [req]
    (info "REQUEST:" req)
    (handler req)))
