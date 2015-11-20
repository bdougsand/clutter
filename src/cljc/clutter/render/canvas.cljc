(ns clutter.render.canvas
  (:require [clutter.render.graphics :as gfx]))

(def pi #?(:cljs js/Math.PI
           :clj Math/PI))

(defprotocol Drawable
  (instructions [x]))

(defprotocol Renderable
  (do-render-into [x elt]))

(defprotocol Styleable)

(defprotocol StyleableLike
  (as-styleable [x]))

(defn get-context [cvs]
  #? (:cljs (.getContext cvs "2d")))

(declare filled? stroked?)

(defn draw-instructions [& children]
  (mapcat (fn [child]
            (let [style-map (:style child)]
              (cond
                (satisfies? Styleable child)
                (if style-map
                  (concat
                   [gfx/save-context
                    gfx/begin-path
                    #(gfx/style % style-map)]
                   (instructions child)
                   (when (filled? style-map)
                     [gfx/fill])
                   (when (stroked? style-map)
                     [gfx/stroke])
                   [gfx/restore-context])

                  (instructions child))

                (satisfies? Drawable child)
                (instructions child)

                (coll? child)
                (mapcat draw-instructions child))))
          children))

(defn render-into [what context]
  (doseq [instr (draw-instructions what)]
    (instr context)))

(defn render-canvas [what canvas]
  (let [ctx (get-context canvas)]
    (doseq [instr (draw-instructions what)]
      (instr ctx))))

(defrecord Canvas [children]
  Drawable
  (instructions [this]
    (mapcat instructions children)))

(defrecord Ellipse [x y xradius yradius rot start end]
  Styleable
  Drawable
  (instructions [this]
    [#(gfx/ellipse % x y xradius yradius rot start end)]))

(defrecord Rectangle [w h]
  Styleable
  Drawable
  (instructions [this]
    [#(gfx/rect % 0 0 w h)]))

(defrecord Shape [points]
  Styleable
  Drawable
  (instructions [this]
    (let [[x y & rpoints] points]
      (concat
       [gfx/begin-path
        #(gfx/move-to % x y)]
       (map (fn [[x y]]
              #(gfx/line-to % x y))
            rpoints)))))

(defn smart-path [& ops]
  )


(defn
  ^{:user-doc "Draw a circle of the given radius."
    :args {'r {:hint "radius"}}}
  circle
  ([r]
   (->Ellipse 0 0 r r 0 0 (* 2 pi)))
  ([x y r]
   (->Ellipse x y r r 0 0 (* 2 pi))))

(defn
  ^{:user-doc "Draw an ellipse with the given x-axis and y-axix lengths."}
  ellipse
  ([x-length y-length]
   (-> Ellipse 0 0 x-length y-length 0 0 0)))

(defn
  ^{:export true}
  rect
  ([w h] (->Rectangle w h))
  ([s] (->Rectangle s s)))

(defn
  ^{:user-doc ""}
  shape
  [& points]
  (->Shape points))

(defn transform-fn [tfn children]
  (concat
   [gfx/save-context tfn]
   (mapcat instructions children)
   [gfx/restore-context]))

(defrecord Translation [x y children]
  Drawable
  (instructions [this]
    (transform-fn #(gfx/translate % x y) children)))

(defrecord Rotation [rad children]
  Drawable
  (instructions [this]
    (transform-fn #(gfx/rotate % rad) children)))

#_
(defn translate [x y & children]
  (->Translation x y children))

(defn transform-all [transforms children]
  (map (fn [child]
         (cond
           (satisfies? Styleable child)
           (assoc child :transform (merge (:transform child) transforms))

           (coll? child)
           (transform-all transforms child)

           :else
           (ex-info "Applying transform to non-transformable."
                    {})))))

(defn
  ^{:user-doc ""}
  translate [x y & children]
  )

(defn rotate [rads & children]
  (->Rotation rads children))

(defn style-instructions [style-map]
  )

(defn filled? [style-map]
  (:fill-style style-map))

(defn stroked? [style-map]
  (or (:stroke-style style-map)
      (:line-width style-map)))


;; (extend-protocol Drawable
;;   cljs.core.PersistentVector
;;   (instructions [v] (mapcat instructions v))

;;   cljs.core.LazySeq
;;   (instructions [ls] (mapcat instructions ls))

;;   cljs.core.List
;;   (instructions [l] (mapcat instructions l)))

(defn style-all [style-map children]
  (map (fn [child]
         (cond
           (satisfies? Styleable child)
           (assoc child :style (merge (:style child) style-map))

           (coll? child)
           (style-all style-map child)

           :else
           (ex-info "Applying styles to non-Styleable:"
                    {:child child})))
       children))


(defn style [style-map & children]
  (style-all style-map children))

(defn line-width [w & children]
  (style-all {:line-width w} children))

(defn
  ^{:user-doc "Apply the given stroke style to the given nodes."}
  stroke [s & children]
  (style-all {:stroke-style (name s)} children))

(defn
  ^{:user-doc ""}
  alpha [a & children]
  (style-all {:alpha a} children))

(defn
  ^{:user-doc ""}
  fill-style [s & children]
  (style-all {:fill-style s} children))

(defn
  ^{:user-doc ""}
  shadow [s & children]
  (style-all {:shadow s} children))

(defn
  ^{:user-doc ""}
  text-baseline [b & children]
  (style-all {:text-baseline b} children))

(extend-protocol Drawable
  #?(:cljs string
     :clj String)
  (instructions [s]
    [#(gfx/fill-text % s 0 0)]))

(defrecord Text [text x y]
  Drawable
  (instructions [this]
    (concat
     (when (:filled this)
       [#(gfx/fill-text % text x y)])
     (when (:outlined this)
       [#(gfx/stroke-text % text x y)]))))

(defn ^{:user-doc "Draw an outline of the given string at the specified
  coordinates relative to the current context."}
  outlined-text [string x y]
  (map->Text {:text string
              :x x
              :y y
              :outlined true}))

(defn ^{:user-doc "Draw the given string filled at the specified coordinates, relative to teh current context."}
  fill-text [string x y]
  (map->Text {:text string
              :x x
              :y y
              :filled true}))
