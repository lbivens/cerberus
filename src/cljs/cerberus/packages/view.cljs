(ns cerberus.packages.view
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
   [cerberus.packages.api :refer [root] :as packages]
   [cerberus.view :as view]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))



(defn render-home [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/h2 (:name data))
       "UUID: " (:uuid data) (d/br)
       "CPU Capacy: " (:cpu_cap data) (d/br)
       "Quota: " (:quota data) (d/br)
       "RAM: " (:ram data) (d/br)
       "Compression: " (:compression data) (d/br)))))

(defn render-requirement [{attribute :attribute condition :condition value :value
                           weight :weight}]
  [(d/dt weight) (d/dd attribute " " condition " " value)])

(defn render-reqs [app owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (map render-requirement app)))))

(def sections
  {""             {:key  1 :fn #(om/build render-home %2)      :title "General"}
   "requirements" {:key  2 :fn #(om/build render-reqs (:requirements %2))  :title "Requirements"}
   "metadata"     {:key  2 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections #(packages/get %2)))
