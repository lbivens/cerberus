(ns jingles.roles.view
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
   [jingles.roles.api :refer [root] :as roles]
   [jingles.view :as view]
   [jingles.metadata :as metadata]
   [jingles.permissions :as permissions]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [fmt-bytes fmt-percent]]))



(defn render-home [app element]
  (pr-str element))

(def sections {""             {:key  1 :fn render-home                       :title "General"}
               "permsissions" {:key  2 :fn #(om/build permissions/render %2) :title "Permissions"}
               "metadata"     {:key  3 :fn #(om/build metadata/render %2)    :title "Metadata"}})

(def render (view/make root sections roles/get))
