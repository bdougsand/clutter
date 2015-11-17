(ns clutter.api
  "The namespace with functions available to client-side programs.")

(defn ^{:export true
        :user-doc "Rounds down to the nearest integer less than x"}
  floor
  [x]
  (.floor js/Math x))

(defn ^{:export true
        :user-doc "Rounds up to the nearest integer greater than x"}
  ceil
  [x]
  (.ceil js/Math x))

(defn ^{:export true
        :user-doc "Returns the absolute value of x"}
  abs
  [x]
  (.abs js/Math x))

(defn ^{:export true
        :user-doc "Trigonometric sine function"}
  sin
  [x]
  (.sin js/Math x))

(defn ^{:export true
        :user-doc "Trigonometric cosine function"}
  cos
  [x]
  (.cos js/Math x))

(defn ^{:export true
        :user-doc "Trigonometric tangent function"}
  tan
  [x]
  (.tan js/Math x))

(def pi (.-PI js/Math))

(def infinity js/Infinity)

(def to-int js/parseInt)
