(ns cerberus.utils
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core.match]
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [cerberus.state]))

(defn tr-color [e]
  (cond
    (> (:raised    e) 0) "danger"
    (> (:confirmed e) 0) "warning"
    (> (:cleared   e) 0) "info"
    :else                "success"))
(defn log [e]
  (. js/console (log e)))

(defn goto [& page]
  (set! (.-hash js/location) (apply str "#" page)))

(defn a [fun e & content]
  (let [path (fun {:id (:id e)})]
    (d/a #js{:href path} (:name e) content)))

(defn by-id [id]
  (. js/document (getElementById id)))

(defn val-by-id [id]
  (if-let [e (by-id id)]
    (.-value e)))

(defn event-val [e]
  (.-value (.-target e)))

(defn ->state [owner key]
  (fn ->state-event [e]
    (om/set-state! owner key (event-val e))))

(defn prevent-default [f]
  (fn [event]
    (f event)
    (.preventDefault event)))

(defn stop-propagation [f]
  (fn [event]
    (f event)
    (.stopPropagation event)))

(defn make-event [f]
  (prevent-default (stop-propagation f)))

(defn initial-state [config]
  (reduce
   (fn [acc e]
     (-> acc
         (assoc-in [e :show]  (get-in config [:fields e :show] true))
         (assoc-in [e :order] (get-in config [:fields e :order] 0))))
   {}
   (keys (:fields config))))

(defn value-by-key [key element]
  (cond
    (keyword? key) (key element)
    (fn? key) (key element)
    (list? key) (get-in element (vec key))
    (vector? key) (get-in element (vec key))
    :else ""))

(defn row [& body]
  (g/row {} body))

(defn grid-row [& body]
  (g/grid {} (apply row body)))

(defn str->int [v]
  (js/parseInt v))

(defn ip->int [ip]
  (let [parts (clojure.string/split ip #"\.")
        [a b c d] (map str->int parts)]
    (bit-or (* 16777216 a) (* 65536 b) (* 256 c) d)))

(defn menu-items [& items]
  (map-indexed
   (fn [idx data]
     (match
      data
      :divider (b/menu-item {:key (inc idx) :divider? true})
      [title target] (if (fn? target)
                       (b/menu-item {:key (inc idx) :on-click (make-event target)} title)
                       (b/menu-item {:key (inc idx) :href target} title))
      [title opts target] (if (fn? target)
                            (b/menu-item (merge {:key (inc idx) :on-click (make-event target)} opts) title)
                            (b/menu-item (merge {:key (inc idx) :href target} opts) title))))
   (filter boolean items)))

(defn vec-or-seq? [e]
  (or (vector? e) (seq? e)))

(defn path-vec [e]
  (cond
    (vector? e) e
    (seq? e)    (vec e)
    :else [e]))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn li [[label value]]
  (d/li {:class "list-group-item"}
        (d/div {:class "span-label"} label)
        (d/div {:class "span-value"} value)))

(defn lg [& items]
  (d/ul
   {:class "list-group"}
   (map li (partition 2 items))))

(defn to-date [date]
  (str (js/Date. (/ date 1000))))

(defn ensure-kw [s]
  (if (string? s)
    (keyword s)
    s))

(def version "0.9.1")
