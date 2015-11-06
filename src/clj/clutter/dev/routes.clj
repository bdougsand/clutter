(ns clutter.dev.routes
  (:require [clojure.string :as str]
            [clutter.dev.docs :as docs]
            [compojure.core :refer [GET defroutes]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.transit :refer [wrap-transit-response]]
            [ring.util.response :refer [response]]))

(def public-vars
  (ns-publics (find-ns 'clojure.core)))

(def var-names
  (map (comp name key) public-vars))

;; TODO: Handle namespaced variables?
(defn symbol-info [s]
  (some-> (public-vars (symbol s))
          (meta)
          (select-keys [:name :arglists :doc])
          (docs/add-arg-docs)))

(defn escape-re [s]
  (str/replace s #"[.()\\^?*]" "\\\\$0"))

(defn completions [s]
  (filter #(re-find (re-pattern (str "^" (escape-re s))) %) var-names))

(defn doc-view [req]
  (response (symbol-info (-> req :params :symbol))))

(defn completions-view [req]
  (response {:completions (vec (completions (-> req :params :symbol)))}))


(defroutes dev-routes
  (GET "docs" req doc-view)
  (GET "hi" req "Hello")
  (GET "compl" req completions-view))
