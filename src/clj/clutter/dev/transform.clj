(ns clutter.dev.transform
  (:require [clojure.walk :refer [postwalk]]))

(def async-methods
  '#{get-prop get-location get-name get-contents get-type})

(def blacklist
  '#{aset aget aclone clj->js js->clj})

(def illegal-pattern #"^(\.|js/)")

(defn rewrite-props [form]
  (postwalk (fn [x]
              (if (and (symbol? x) (= (subs (name x) 0 1) "&"))
                (list 'get-prop 'this (keyword (subs (name x) 1)))
                x))
            form))

(defn rewrite-async [form]
  (postwalk (fn [x]
              (if (and (list? x) (symbol? (first x)) (async-methods (first x)))
                (list '<! x)
                x))
            form))

(defn rewrite-js [form]
  (postwalk (fn [x]
              (if (symbol? x)
                (cond
                  (or (blacklist x)
                      (re-find illegal-pattern (str x)))
                  (throw (ex-info "IllegalCall" {:symbol x}))

                  :default x)
                x))
            form))

(defn transform [form]
  (rewrite-js form))

(defn body-transform [form]
  (list 'go (-> form
                (rewrite-props)
                (rewrite-async))))

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
