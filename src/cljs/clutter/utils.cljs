(ns clutter.utils
  (:require [goog.dom :as dom]
            [goog.dom.classes :as classes]
            [goog.string :as string]
            [goog.string.linkify :as linkify]
            [goog.style :as style]))

;; Make HTML types seqable.
(extend-protocol ISeqable
  js/NodeList
  (-seq [nl] (array-seq nl 0))

  js/HTMLCollection
  (-seq [nl] (array-seq nl 0))

  js/FileList
  (-seq [fl] (array-seq fl 00)))

(defn by-id
  [id]
  (.getElementById js/document id))

(defn scroll-to-bottom!
  [elt]
  (set! (.-scrollTop elt) (.-scrollHeight elt)))

(defn scroll-dist-from-bottom
  [elt]
  (- (.-scrollHeight elt)
     (+ (.-scrollTop elt) (.-offsetHeight elt))))

(defn get-input
  [elt n]
  (let [n (name n)]
    (first
     (filter #(= (.-name %) n)
             (dom/getElementsByTagNameAndClass "input" nil elt)))))

(defn input? [elt]
  (or (and (= (.-tagName elt) "INPUT")
           (= "text" (.-type elt)))
      (= (.-tagName elt) "TEXTAREA")))

(defn get-named-val
  [elt n]
  (when-let [input (get-input elt n)]
    (.-value input)))

(defn cancel
  "Stops propagation of an event and prevents the default handler."
  [e]
  (.stopPropagation e)
  (.preventDefault e))


;; Debugging help:
(defn prn-through [x]
  (prn x)
  x)

(def url-re (js/RegExp linkify/URL_ "gi"))

(defn match-seq [re s]
  (if-let [m (.exec re s)]
    (if-let [pre (subs s 0 (.-index m))]
      (cons
       pre
       (cons m (lazy-seq
                (match-seq re (subs s (+ (.-index m) (count (aget m 0)))))))))

    [s]))

(defn match-urls [s]
  (match-seq url-re s))

(defn should-scroll? [elt]
  (let [lc-height (or (some-> (dom/getLastElementChild elt)
                              (style/getSize)
                              .-height)
                      15)]
    (<= (scroll-dist-from-bottom elt) (+ lc-height 5))))
