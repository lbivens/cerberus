(ns cerberus.metrics
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [cerberus.utils :refer [row]]))

;; This is really ugly but something is crazy about the reify for OM here
;; this for will moutnt and will unmoutn are not the same and having timer in
;; let does not work either so lets "MAKE ALL THE THINGS GLOBAL!"

(def timer (atom))

(defn stop-timer!
  ([]
   (if @timer
     (js/clearInterval @timer))
   (reset! timer nil))
  ([local-timer]

   (if @local-timer
     (js/clearInterval @local-timer))
   (stop-timer!)))

(defn start-timer! [tick]
  (stop-timer!)
  (let [local-timer (atom)
        t (js/setInterval #(tick local-timer) 1000)]
    (reset! local-timer t)
    (reset! timer t)))

(defn max-metric [[_ {points :points}]]
  (apply max points))

(defn  normalize-metric [max [name {points :points}]]
  (if (= max 0)
    [name points]
    [name (map #(* (/ (- max  %) max) 100) points)]))

(defn normalize-metrics [[name metrics]]
  (let [max (apply max (map max-metric metrics))]
    {:name name
     :max max
     :unit (:unit  (second (first metrics)))
     :lines (map (partial normalize-metric max) metrics)}))

(defn mkp [points]
  (apply str (map (fn [[x y]] (str x "," y " ")) points)))


(defn omg [{name :name lines :lines max :max unit :unit} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "omg")
    om/IRender
    (render [this]
      (let [x 20
            y 10]
        (d/svg
         {:class   "omg"
          :viewBox "0 0 200 80"}
         ;; max text
         (d/text {:x 10 :y -15 :class "label max"} (str (Math/round max) " " unit))
         ;; min text
         (d/text {:x 10  :y 90  :class "label min"} 0 )
         (d/text {:x 10  :y 100 :class "label min"} "- 1 min")
         (d/text {:x 130 :y 100 :class "label min"} "now")
         ;; x-line
         (d/polyline
          {:points (mkp [[(- x 4) (- 102 y)] [(+ 120 x) (- 102 y)]])
           :class "axis x"})
         ;;y - line
         (d/polyline
          {:points (mkp [[(- x 4) (* -1 y)] [(- x 4) (- 97 (/ y 2))]])
           :class "axis y"})
         (map-indexed
          (fn [idx [line points]]
            (d/polyline
             {:points (mkp (map-indexed (fn [a b] [(+ (* a 2) x) (- b 10)]) points))
              :class  (str "line line-" line " line-" idx)
              :style {:fill "none"}}))
          lines))))))

(defn point-view [{name :name :as data} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "metric-view")
    om/IRender
    (render [this]
      (g/col
       {:xs 12 :sm 6 :md 4 :lg 3
        :style {:text-align "center"}}
       (p/panel
        {:header name
         :class  name}
        (om/build omg data))))))

(defn process-metric [{name :n points :v}]
  {:name (clojure.string/split (clojure.string/replace name "'" "") #"-")
   :points points})

(defn render [data owner {build-metric :translate}]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (if (or (not data) (= data :no-metrics)
               (= data :pending))
         (row
          (g/col
           {:xs 12}
           (d/p "No metric storage seems to be configured please install DalmatinerDB and Tachyon to use this feature")))
         (row
          (let [metrics (map process-metric data)
                metrics (reduce build-metric {} metrics)
                metrics (map normalize-metrics metrics)]
            (om/build-all point-view metrics))))))))
