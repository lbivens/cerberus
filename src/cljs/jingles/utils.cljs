(ns jingles.utils
  (:require [om-tools.dom :as d :include-macros true]
            [jingles.state]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]))

(defn goto [& page]
  (set! (.-hash js/location) (apply str "#" page)))

(defn by-id [id]
  (. js/document (getElementById id)))

(defn val-by-id [id]
  (.-value (by-id id)))

(defn a [url name]
  (d/a #js{:href url} name))
