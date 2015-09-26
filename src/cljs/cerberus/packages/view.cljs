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

(defn render-requirement [{:keys [attribute condition value
                                  weight low high]}]
  (condp = weight
    "scale" [(d/dt weight) (d/dd (d/strong attribute) " betwee " (d/strong  low) " and " (d/strong high))]
    "random" [(d/dt weight) (d/dd "between " (d/strong low) " and " (d/strong high))]
    [(d/dt weight) (d/dd (d/strong attribute) " " condition " " (d/strong value))]))

(defn build-reqs [reqs]
  (d/dl
   {}
   (map render-requirement reqs)))
(defn render-reqs [app owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (build-reqs app)))))

(def sections
  {""             {:key  1 :fn #(om/build render-home %2)      :title "General"}
   "requirements" {:key  2 :fn #(om/build render-reqs (:requirements %2))  :title "Requirements"}
   "metadata"     {:key  3 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections packages/get :name-fn :name))
