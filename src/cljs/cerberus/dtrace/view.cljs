(ns cerberus.dtrace.view
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
   [cerberus.utils :refer [goto grid-row display]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.dtrace.api :as dtrace]
   [cerberus.dtrace.api :refer [root]]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.view :as view]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn render-home [app element]
  (pr-str element))

(def sections {""          {:key  1 :fn render-home      :title "General"}
               "metadata"  {:key  2 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render
  (view/make
   root sections
   dtrace/get-page
   :name-fn :name))
