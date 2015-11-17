(ns clutter.render.canvas)

(defprotocol Drawable
  (instructions [x]))

(defprotocol Renderable
  (do-render-into [x elt]))

(defn get-context [cvs]
  (.getContext cvs "2d"))

(defn render-into [what canvas]
  (if (satisfies? Renderable what)
    (let [ctx (get-context canvas)]
      (doseq [instr (instructions what)]
        ))))


(defrecord Canvas [children]
  Drawable
  (instructions [this]
    (mapcat instructions children)))

(defrecord Ellipse [xradius yradius rot start end]
  Drawable
  (instructions [this]
    #(.ellipse % 0 0 xradius yradius rot start end)))

(defrecord Rectangle [w h]
  Drawable
  (instructions [this]
    ))


(defn
  ^{:export true
    :user-doc "Draw a circle of the given radius."
    :args {'r {:hint "radius"}}}
  circle [r]
  (->Ellipse r r 0 0 0))

(defn
  ^{:export true
    :user-doc ""}
  rect
  ([w h] (Rectangle. w h))
  ([s] (Rectangle. s s)))

(defn transform [tfn children]
  (concat
   [#(.save %) tfn]
   (mapcat instructions children)
   [#(.restore %)]))

(defrecord Translation [x y children]
  Drawable
  (instructions [this]
    (transform #(.translate x y) children)))

(defrecord Rotation [rad children]
  Drawable
  (instructions [this]
    (transform #(.rotate % rad) children)))

(defn translate [x y & children]
  (->Translation x y children))

(defn rotate [rads & children]
  (->Rotation rads children))

;; TODO: Some additional processing of arguments
(def styles {:line-width "lineWidth"
             :stroke-style "strokeStyle"
             :alpha "globalAlpha"
             :fill-style "fillStyle"
             :shadow "shadow"
             :text-align "textAlign"
             :text-baseline "textBaseline"
             :font "font"})

(defrecord Style [children]
  Drawable
  (instructions [this]
    (transform
     (fn [ctx]
       (doseq [[k att] styles]
         (when-let [v (k this)]
           (aset ctx att v))))
     children)))

(defn style [style-map & children]
  (map->Style (assoc style-map :children children)))

(defn line-width [w & children]
  (map->Style {:line-width w
               :children children}))

(defn
  ^{:user-doc "Apply the given stroke style to the given nodes."}
  stroke-style [s & children]
  (map->Style {:stroke-style s
               :children children}))

(extend-protocol Drawable
  #?(:cljs string
     :clj String)
  (instructions [s]
    [#(.fillText % s 0 0)]))

(defrecord Text [text x y]
  Drawable
  (instructions [this]
    (concat
     (when (:filled this)
       [#(.fillText % text x y)])
     (when (:outlined this)
       [#(.strokeText % text x y)]))))

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
