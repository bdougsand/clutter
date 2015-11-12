(ns clutter.dev.compile
  (:require [cljs.core]
            [cljs.compiler :as compiler]
            [cljs.analyzer :as analyzer]
            [cljs.env :as env]

            [clutter.dev.names :as names]))

(defn analyze
  ""
  ([expr ns-str locals]
     (let [ns-sym (symbol ns-str)]
       (binding [analyzer/*cljs-ns* ns-sym]
         (let [env {:ns {:name ns-sym}
                    :uses #{'cljs.core}
                    :context :expr
                    :locals (merge names/all-names
                                   locals)
                    :excludes '#{binding aget aset}}]
           (with-redefs [analyzer/get-expander exp]
             (analyzer/analyze env expr))))))
  ([expr ns-str]
     (analyze expr ns-str {})))
