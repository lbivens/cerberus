(ns jingles.vms
  (:require [jingles.api :as api]
            [jingles.list :as jlist]
            [jingles.state :refer [set-state!]]))

(def root :vm)

(def config {:fields {:name {:id :name :title "Name" :key '(:config :alias)}
                      :uuid {:id :uuid :title "UUID" :key :uuid}}
             :root root
             :title "Machines"})

(set-state! [root :fields] (keys (:fields config)))

(def list-fields
  "alias,uuid,config")

(defn full-list []
  (api/to-state [:vm :list] (api/full-list "vms" list-fields)))

(defn list-view [app]
  (jlist/view config app))
