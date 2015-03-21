(ns jingles.list
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]))

(defn show-field [field element]
  (let [key (:key field)]
    (cond
     (keyword? key) (key element)
     (fn? key) (key element)
     (vector? key) (get-in element key))))

(defn tbl-headers [fields]
  (d/thead
   (d/tr
    (map #(d/td (:title %)) fields))))

(defn tbl [fields elements]
  (table
    {:striped? true :bordered? true :condensed? true :hover? true}
    (tbl-headers fields)
    (d/tbody
           (map
            (fn [e] (d/tr
                     (map
                      (fn [field]
                        (d/td (show-field field e)))
                      fields)))
            elements))))
