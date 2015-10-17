(ns clutter.renderable
  (:require [goog.string :as string]))

(defprotocol IEscape
  (escaped [s]))

(deftype EscapedString [s]
  IEscape
  (escaped [_] s)

  Object
  (toString [_] s))

(extend-protocol IEscape
  string
  (escaped [s] (->EscapedString (string/htmlEscape s)))

  object
  (escaped [o] (escaped (str o))))

(defn mark-safe
  ([s] (->EscapedString s))
  ([s & s2 ss] (->EscapedString (apply str s s2 ss))))

(defprotocol IRenderable
  (render [x]))

(defrecord Image [source width height]
  IRenderable
  (render [img]
    #_(mark-safe )))
