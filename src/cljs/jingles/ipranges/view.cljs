(ns jingles.ipranges.view
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [jingles.view :as view]
   [jingles.metadata :as metadata]
   [jingles.ipranges.api :refer [root] :as ipranges]))



(defn render-home [app element]
  (pr-str element))

(def sections {""          {:key  1 :fn render-home      :title "General"}
               "metadata"  {:key  2 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections ipranges/get))
