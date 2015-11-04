(ns clutter.editor.docs
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol DocProvider
  (docs-for-symbol [this sym]))

(defprotocol Documentation
  (doc-symbol [x])
  )

(defrecord DefaultDocProvider [cache]
  DocProvider
  (docs-for-symbol [this sym]
    (go
      (if-let [body (get @cache sym)]
        body

        (let [resp (<! (http/get "/docs" {:query-params {:symbol (str sym)}}))]
          (when (= (:status resp) 200)
            (:body resp)))))))


(defn default-docs []
  (->DefaultDocProvider (atom {})))
