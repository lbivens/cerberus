(ns cerberus.datasets.view
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
   [cerberus.datasets.api :refer [root] :as datasets]
   [cerberus.view :as view]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn render-home [app element]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/p (:description app))
       (d/ul
        (d/li "type: " (:type app))
        (d/li "version: " (:version app)))))))

(defn render-requirement [{attribute :attribute condition :condition value :value
                           weight :weight}]
  [(d/dt weight) (d/dd attribute " " condition " " value)])

(defn requirements [app element]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/ul
        (map render-requirement app))))))

(defn networks [app element]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/dl
        (map (fn [{desc :description name :name}]
               [(d/dt name) (d/dd desc)]) app))))))



(def sections {""              {:key  1 :fn #(om/build render-home %2)      :title "General"}
               "requirements"  {:key  2 :fn #(om/build requirements (:requirements %2))     :title "Requirements"}
               "networks"      {:key  3 :fn #(om/build networks (:networks %2))         :title "Networks"}
               "metadata"      {:key  4 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render
  (view/make root sections datasets/get
             :name-fn #(if-let [homepage (get-in % [:metadata :homepage])]
                         (d/a {:href homepage} (:name %))
                         (:name %))))
