(ns jingles.vms
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.vms.api :refer [root]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [jingles.vms.api :as vms]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.list :as jlist]
   [jingles.vms.view :as view]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.fields :refer [mk-config]]
   [jingles.state :refer [set-state!]]))

(defn actions [e]
  (let [uuid (:uuid e)
        locked (get-in e [:metadata :jingles :locked] false)
        set-lock (partial vms/update-metadata uuid [:jingles :locked])]
    [(if locked
       ["Unlock" #(set-lock false)]
       ["Lock" #(set-lock true)])
     :divider
     (if (= (:state e) "running")
       ["Stop" {:class (if locked "disabled")} #(vms/stop uuid)]
       ["Start" {:class (if locked "disabled")} #(vms/start uuid)])
     (if (= (:state e) "running")
       ["Reboot" {:class (if locked "disabled")} #(vms/reboot uuid)])]))

(def config (mk-config
             root "Machines" actions
             :name {:title "Name" :key '(:config :alias) :order -10}
             :cpu {:title "CPU" :key '(:config :cpu_cap) :type :percent}
             :state {:title "State" :key :state :type :string}
             :dataset {:title "Dataset" :type :string
                       :key (partial api/get-sub-element :datasets :dataset
                                     #(str (:name %) "-" (:version %)))}
             :package {:title "Package" :type :string
                       :key (partial api/get-sub-element :packages :package [:name])}))

(set-state! [root :fields] (initial-state config))

(defn render [app]
  (condp = (:view app)
    :list (jlist/view config app)
    :show (view/render app)))
