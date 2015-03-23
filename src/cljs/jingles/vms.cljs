(ns jingles.vms
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.api :as api]
            [jingles.http :as http]
            [jingles.list :as jlist]
            [jingles.vms.view :as view]
            [jingles.state :refer [set-state!]]))

(def root :vms)


(def config {:fields {:name {:id :name :title "Name" :key '(:config :alias)}
                      :uuid {:id :uuid :title "UUID" :key :uuid}
                      :state {:id :state :title "State" :key :state}
                      :dataset {:id :dataset :title "Dataset"
                                :key (partial api/get-sub-element :datasets :dataset [:name])}
                      :package {:id :package :title "Package"
                                :key (partial api/get-sub-element :packages :package [:name])}}
             :root root
             :title "Machines"})

(set-state! [root :fields] (reduce #(assoc-in %1 [%2 :show] true) {} (keys (:fields config))))

(def list-fields
  "alias,uuid,config,state,dataset,package")

(def list (partial api/list root list-fields))

(def get (partial api/get root))

(defn list-view [app]
  (jlist/view config app))

(defn show-view [app]
  (. js/JSON (stringify (clj->js (get-in app [root :element])))))

(defn render [app]
  (condp = (:view app)
    :list (list-view app)
    :show (view/render app)))
