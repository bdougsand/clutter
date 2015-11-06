(ns clutter.editor.autocomplete
  (:require [cljs.core.async :as async :refer [<!]]
            [cljs-http.client :as http]
            [clojure.string :as str]
            [clutter.editor.buffer :as b])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn get-completions [pref]
  (go
    (let [resp (<! (http/get "/compl" {:query-params {:symbol pref}}))]
      (if (= (:status resp) 200)
        (:body resp)))))

(defn do-complete [cm cb]
  (let [w (some-> (b/word-at cm) (str/trim))]
    (get-completions w)))

(set! (.-async do-complete) true)
