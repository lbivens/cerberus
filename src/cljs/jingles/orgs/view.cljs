(ns jingles.orgs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [jingles.utils :refer [goto grid-row display]]
   [jingles.http :as http]
   [jingles.api :as api]
   [jingles.orgs.api :refer [root] :as orgs]
   [jingles.view :as view]
   [jingles.services :as services]
   [jingles.metadata :as metadata]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [fmt-bytes fmt-percent]]))


(defn render-home [app element]
  (pr-str element))

(def sections {""          {:key  1 :fn render-home      :title "General"}
               "metadata"  {:key  2 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections orgs/get))

