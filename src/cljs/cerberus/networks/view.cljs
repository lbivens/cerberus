(ns cerberus.networks.view
  (:require
   [om.core :as om :include-macros true]
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
   [cerberus.networks.api :refer [root] :as networks]
   [cerberus.networks.api]
   [cerberus.view :as view]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn render-ranges [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/ul
        (map d/li (:ipranges data)))))))

(defn render-home [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/h2 (:name data))
       "UUID: " (:uuid data) (d/br)))))

(def sections
  {""          {:key  1 :fn #(om/build render-home %2)     :title "General"}
   "ipranges"  {:key  2 :fn #(om/build render-ranges %2)   :title "IP Ranges"}
   "metadata"  {:key  3 :fn #(om/build metadata/render %2) :title "Metadata"}})

(def render (view/make root sections #(networks/get %2)))

