(ns jingles.vms
  (:refer-clojure :exclude [get list])
  (:require [jingles.api :as api]
            [jingles.list :as jlist]
            [jingles.state :refer [set-state!]]))

(def root :vms)

(def config {:fields {:name {:title "Name" :key '(:config :alias)}
                      :uuid {:title "UUID" :key :uuid}
                      :state {:title "State" :key :state}}
             :root root
             :title "Machines"})

(set-state! [root :fields] (keys (:fields config)))

(def list-fields
  "alias,uuid,config,state")

(def list (partial api/list root list-fields))

(def get (partial api/get root))

(defn list-view [app]
  (jlist/view config app))

(defn show-view [app]
  (. js/JSON (stringify (clj->js (get-in app [root :element])))))

(defn view [app]
  (condp = (:view app)
    :list (list-view app)
    :show (show-view app)))

