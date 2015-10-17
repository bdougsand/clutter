(ns clutter.api
  "The namespace with functions available to client-side programs.")

(defn floor ^{:export true
              :user-doc "Rounds down to the nearest integer less than x"}
  [x]
  (.floor js/Math x))

(defn ceil ^{:export true
             :user-doc "Rounds up to the nearest integer greater than x"}
  [x]
  (.ceil js/Math x))

(defn abs ^{:export true
            :user-doc "Returns the absolute value of x"}
  [x]
  (.abs js/Math x))

(defn sin ^{:export true
            :user-doc "Trigonometric sine function"}
  [x]
  (.sin js/Math x))

(defn cos ^{:export true
            :user-doc "Trigonometric cosine function"}
  [x]
  (.cos js/Math x))

(defn tan ^{:export true
            :user-doc "Trigonometric tangent functino"}
  [x]
  (.tan js/Math x))

(def pi (.-PI js/Math))

(def infinity js/Infinity)

(def to-int js/parseInt)
