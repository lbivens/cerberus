(ns jingles.vms
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.vms.api :refer [root]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.list :as jlist]
   [jingles.vms.view :as view]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.fields :refer [mk-config]]
   [jingles.state :refer [set-state!]]))


(defn actions [e]
  (let [locked (get-in e [:metadata :jingles :locked] false)
        set-lock (partial api/update-metadata root (:uuid e) [:jingles :locked])]
    [(if locked
       ["Unlock" #(set-lock false)]
       ["Lock" #(set-lock true)])
     :divider
     (if (= (:state e) "running")
       ["Stop" {:class (if locked "disabled")} pr]
       ["Start" {:class (if locked "disabled")} pr])
     (if (= (:state e) "running")
       ["Restart" {:class (if locked "disabled")} pr])]))

(def config (mk-config
             root "Machines" actions
             :cpu {:id :cpu :title "CPU" :key '(:config :cpu_cap) :type :percent}
             :state {:id :state :title "State" :key :state :type :string}
             :dataset {:id :dataset :title "Dataset" :type :string
                       :key (partial api/get-sub-element :datasets :dataset
                                     #(str (:name %) "-" (:version %)))}
             :package {:id :package :title "Package" :type :string
                       :key (partial api/get-sub-element :packages :package [:name])}))

(set-state! [root :fields] (initial-state config))

(defn render [app]
  (condp = (:view app)
    :list (jlist/view config app)
    :show (view/render app)))
