(ns clutter.dev.compile
  (:require [clojure.core.async :refer [put!]]
            [clojure.tools.reader.edn :as edn]

            [clutter.dev.transform :as trans]
            #_[clutter.dev.build :as build]))


(defn compile
  [c job]
  (let [{:keys [source id ns]} job]

    (as-> source x
          (edn/read-string x)
          )))
