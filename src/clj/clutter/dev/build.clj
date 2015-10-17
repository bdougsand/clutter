(ns clutter.build.build
  (:require [cljs.core]
            [cljs.compiler :as compiler]
            [cljs.analyzer :as analyzer]
            [cljs.env :as env]
            [clojure.string :as str]

            [clutter.dev.names :as names]))

;; Early versions of this code were copied wholesale from Himera:
;; https://github.com/fogus/himera/blob/master/src/clj/himera/server/cljs.clj

(declare exp)


;; TODO: Prevent users from calling accessing js/*!
;; (Very important!)
(defn build
  ""
  ([expr ns-str locals]
     (let [ns-sym (symbol ns-str)]
       (binding [analyzer/*cljs-ns* ns-sym
                 compiler/*source-map-data* (atom {:source-map (sorted-map)
                                                   :gen-col 0
                                                   :gen-line 0})]
         (let [env {:ns {:name ns-sym
                         :requires-macros names/lib-macros}
                    :uses #{'cljs.core}
                    :context :expr
                    :locals (merge (names/load-names)
                                   locals)
                    :excludes '#{binding aget aset}}]
           (with-redefs [analyzer/get-expander exp]
             (prn compiler/*source-map-data*)
             (compiler/emit-str (analyzer/analyze env expr)))))))
  ([expr ns-str]
     (build expr ns-str {})))

(defn- exp  [sym env]
  (let [mvar
        (when-not (or (-> env :locals sym)        ;locals hide macros
                      (-> env :ns :excludes sym))
          (if-let [nstr (namespace sym)]
            (when-let [ns (cond
                            (= "clojure.core" nstr) (find-ns 'cljs.core)

                            (.contains nstr ".") (find-ns (symbol nstr))

                            :else
                            (-> env :ns :requires-macros (get (symbol nstr))))]
              (.findInternedVar ^clojure.lang.Namespace ns (symbol (name sym))))

            ;; Not a namespaced symbol:
            (if-let [nsym (-> env :ns :uses-macros sym)]
              (.findInternedVar ^clojure.lang.Namespace (find-ns nsym) sym)
              (.findInternedVar ^clojure.lang.Namespace (find-ns 'cljs.core) sym))))]
    (let [sym (symbol (.getName sym))]
      (when (and mvar (or (names/clojure-macros sym) (names/cljs-macros sym)))
        @mvar))))

(defn namespace-for-name-and-id
  [name id]
  (-> name
      (str/replace #"[!@#$%^&*()]+" "")
      (str/replace #"\s+" "_")
      (str "_" id)))
