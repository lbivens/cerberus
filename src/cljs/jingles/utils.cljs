(ns jingles.utils
  (:require [om-tools.dom :as d :include-macros true]
            [jingles.state]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]))

(defn tr-color [e]
  (cond
   (> (:raised e) 0) "danger"
   (> (:confirmed e) 0) "warning"
   (> (:cleared e) 0) "info"
   :else "success"))

(defn goto [& page]
  (set! (.-hash js/location) (apply str "#" page)))

(defn state [e]
  (d/span
   "(" (r/label {:bs-style "danger"} (:raised e))
   "/" (r/label {:bs-style "warning"} (:confirmed e))
   "/" (r/label {:bs-style "success"} (:cleared e))
   ")"))

(defn a [fun e & content]
  (let [path (fun {:id (:id e)})]
    (d/a #js{:href path} (:name e) content)))


(defn child-list [hdr link-fn elements]
  (p/panel
   {:header hdr}
   (table
    {:striped? true :bordered? true :condensed? true :hover? true}
    (d/thead
     (d/tr
      (d/td "Name")
      (d/td "Alerts")
      (d/td "Confirmed")
      (d/td "Cleared")))
    (d/tbody
           (map
            #(d/tr
              #js{:className (tr-color %)}
              (d/td (a link-fn %))
              (d/td (:raised %))
              (d/td (:confirmed %))
              (d/td (:cleared %)))
            elements)))))

(defn main-list [hdr link-fn elements]
  (g/grid
   {}
   (g/row
    {}
    (g/col {:md 18}
           (child-list hdr link-fn elements)))))

(defn by-id [id]
  (. js/document (getElementById id)))

(defn val-by-id [id]
  (.-value (by-id id)))

(defn log [& parts]
  (. js/console (log parts)))
