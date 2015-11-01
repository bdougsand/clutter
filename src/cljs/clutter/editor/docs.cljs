(ns clutter.editor.docs
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol DocProvider
  (docs-for-symbol [this sym]))

(defrecord DefaultDocProvider []
  DocProvider
  (docs-for-symbol [this sym]
    (go
      (let [resp (<! (http/get "/docs/" {:query-params {:symbol sym}}))]
        (when (= (:status resp) 200)
          (:body resp))))))
