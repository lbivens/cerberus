(ns jingles.datasets
  (:require [jingles.api :as api]
            [jingles.list :as jlist]
            [jingles.state :refer [set-state!]]))

(def root :dataset)

(def config {:fields {:name    {:title "Name" :key :name}
                      :version {:title "Version" :key :version}
                      :uuid    {:title "UUID" :key :uuid}}
             :root root
             :title "Datasetse"})
(set-state! [root :fields] (keys (:fields config)))



(def list-fields
  "name,uuid,version")

(defn full-list []
  (api/to-state [:dataset :list] (api/full-list "datasets" list-fields)))

(defn list-view [app]
  (jlist/view config app))
