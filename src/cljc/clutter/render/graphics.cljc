(ns clutter.render.graphics
  #?(:clj (:import [java.awt Graphics2D])))

;; TODO: Some additional processing of arguments
(def styles
  #?(:cljs
     {:line-width "lineWidth"
      :stroke-style "strokeStyle"
      :alpha "globalAlpha"
      :fill-style "fillStyle"
      :shadow "shadow"
      :text-align "textAlign"
      :text-baseline "textBaseline"
      :font "font"}

     :clj {}))

(defprotocol Context2D
  (ellipse [this x y x-radius y-radius rot start end])
  (save-context [this])
  (restore-context [this])
  #_(transform [this])
  (translate [this x y])
  (rotate [this rad])
  (fill-text [this string x y])
  (stroke-text [this string x y])
  (clear-rect [this x y w h])
  ;; Paths:
  (begin-path [this])
  (close-path [this])
  (fill [this])
  (stroke [this])
  (move-to [this x y])
  (line-to [this x y])
  (bezier-to [this cp1x cp1y cp2x cp2y x y])
  (rect [this x y w h])
  (style [this stylemap]))

#?(:cljs
   (extend-protocol Context2D
     js/CanvasRenderingContext2D
     (save-context [this]
       (.save this))
     (restore-context [this]
       (.restore this))
     (translate [this x y]
       (.translate this x y))
     (rotate [this rad]
       (.rotate this rad))
     (clear-rect [this x y w h]
       (.clearRect this x y w h))
     (fill-text [this s x y]
       (.fillText this s x y))
     (stroke-text [this s x y]
       (.strokeText this s x y))
     (begin-path [this] (.beginPath this))
     (close-path [this] (.closePath this))
     (stroke [this] (.stroke this))
     (fill [this] (.fill this))
     (move-to [this x y] (.moveTo this x y))
     (line-to [this x y] (.lineTo this x y))
     (bezier-to [this cp1x cp1y cp2x cp2y x y]
       (.bezierCurveTo this cp1x cp1y cp2x cp2y x y))
     (rect [this x y w h]
       (.rect this x y w h))
     (ellipse [ctx x y x-radius y-radius rot start end]
       (.ellipse ctx x y x-radius y-radius rot start end))
     (style [this stylemap]
       (doseq [[skey sval] stylemap]
         (when-let [prop (styles skey)]
           (aset this prop sval))))))

#?(:clj
   (deftype ContextWrapper [^Graphics2D g2d ^:volatile-mutable stack]
     Context2D
     (save-context [this]
       (set! stack (cons (.getTransform g2d) stack)))
     (restore-context [this]
       (let [[tr & trs] stack]
         (when tr
           (set! stack trs)
           (.setTransform g2d tr))))
     (translate [this x y]
       (.translate g2d x y))
     (rotate [this rad]
       (.rotate g2d rad))
     (line-to [this x y]
       (.drawLine g2d 0 0 x y))
     (rect [this x y w h]
       (.drawRect g2d x y w h))
     (ellipse [this x y x-radius y-radius rot start end])
     (style [this stylemap])))

(defprotocol Logger
  (get-log [this]))

(deftype DummyContext [^:volatile-mutable log]
  Logger
  (get-log [this] log)

  Context2D
  (save-context [this]
    (set! log (conj log 'save-context)))
  (restore-context [this]
    (set! log (conj log 'restore-context)))
  (begin-path [this]
    (set! log (conj log 'begin-path)))
  (translate [this x y]
       (set! log (conj log ['translate x y])))
  (rotate [this rad]
    (set! log (conj log ['rotate rad])))
  (fill-text [this s x y]
    (set! log (conj log ['fill-text s x y])))
  (rect [this x y w h]
    (set! log (conj log ['rect x y w h])))
  (ellipse [this x y x-radius y-radius rot start end]
    (set! log (conj log ['ellipse x y x-radius y-radius rot start end])))
  (style [this stylemap]
    (set! log (conj log ['style stylemap])))
  (stroke [this]
    (set! log (conj log 'stroke))))
