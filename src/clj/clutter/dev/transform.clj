(ns clutter.dev.transform
  (:require [clojure.walk :refer [postwalk]]))

(def async-methods
  '#{get-prop get-location get-name get-contents get-type})

(defn rewrite-props [form]
  (postwalk (fn [x]
              (if (and (symbol? x) (= (subs (name x) 0 1) "&"))
                (list 'get-prop 'this (subs (name x) 1))
                x))
            form))

(defn rewrite-async [form]
  (postwalk (fn [x]
              (if (and (list? x) (symbol? (first x)) (async-methods (first x)))
                (list '<! x)
                x))
            form))

(defn body-transform [form]
  (list 'go (list '<! (-> form
                          (rewrite-props)
                          (rewrite-async)))))

(defn prop-transform
  "Transforms the source code for a prop into a function form."
  [form]
  (list 'fn '[this me] (body-transform form)))



(comment
  (rewrite-async
   '(str "You wave to " (get-name 3) "!"))

  (body-transform
   '(str "You " (some-> &how (str " ")) "wave to " (get-name 3) "!"))

  (prop-transform
   '(str "You " (some-> &how (str " ")) "wave to " (get-name 3) "!"))

  )
