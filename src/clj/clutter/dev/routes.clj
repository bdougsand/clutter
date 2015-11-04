(ns clutter.dev.routes
  (:require [clutter.dev.docs :as docs]
            [compojure.core :refer [GET defroutes]]
            [ring.middleware.transit :refer [wrap-transit-response]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]))

(def public-vars
  (ns-publics (find-ns 'clojure.core)))

(defn symbol-info [s]
  (some-> (public-vars (symbol s))
          (meta)
          (dissoc :ns :added :line :column :file)
          (assoc (docs/add-arg-docs))))

(defn doc-view [req]
  (response (symbol-info (-> req :params :symbol))))


(defroutes dev-routes
  (GET "eh/" req "Yo")
  (GET "docs/" req doc-view))
