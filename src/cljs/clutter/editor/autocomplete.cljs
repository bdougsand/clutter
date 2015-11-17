(ns clutter.editor.autocomplete
  (:require [cljs.core.async :as async :refer [<!]]
            [cljs-http.client :as http]
            [clojure.string :as str]
            [clutter.editor.buffer :as b])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn get-completions [pref]
  (go
    (let [resp (<! (http/get "/compl" {:query-params {:symbol pref}}))]
      (js/console.log "completions:" (clj->js (:body resp)))
      (if (= (:status resp) 200)
        (:body resp)))))

(defn make-completion [])

#_
(defn do-complete [cm cb]
  (js/console.log cm)
  (go
    (let [w (some-> (b/word-at cm) (str/trim))
          {:keys [completions]} (<! (get-completions w))]
      (js/console.log "got-completions:" (clj->js {:list completions}))
      (cb (clj->js {:list completions})))))
#_
(set! (.-async do-complete) true)

(defn do-complete [cm cb]
  #js ["hello" "world"])
