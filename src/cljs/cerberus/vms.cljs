(ns cerberus.vms
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.vms.api :refer [root]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [cerberus.vms.api :as vms]
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.list :as jlist]
   [cerberus.vms.view :as view]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.fields :refer [mk-config]]
   [cerberus.state :refer [set-state!]]))

(defn actions [e]
  (let [uuid (:uuid e)
        locked (get-in e [:raw :metadata :cerberus :locked] false)
        set-lock (partial vms/update-metadata uuid [:cerberus :locked])
        delete #(vms/delete uuid)
        state (get-in e [:raw :state])]
    [(if locked
       ["Unlock" #(set-lock false)]
       ["Lock" #(set-lock true)])
     :divider
     (if (= state "running")
       ["Stop" {:class (if locked "disabled")} #(vms/stop uuid)]
       ["Start" {:class (if locked "disabled")} #(vms/start uuid)])
     (if (= state "running")
       ["Reboot" {:class (if locked "disabled")} #(vms/reboot uuid)])
     :divider
     ["Delete" {:class (if locked "disabled")} #(vms/delete uuid)]]))

(defn get-ip [vm]
  (:ip (first (filter (fn [{p :primary}] p) (get-in vm [:config :networks])))))

(def config
  (mk-config
   root "Machines" actions
   :name       {:title "Name" :key [:config :alias] :order -10}
   :cpu        {:title "CPU" :key [:config :cpu_cap] :type :percent}
   :ram        {:title "Memory" :key [:config :ram] :type [:bytes :mb]}
   :ip         {:title "IP" :key get-ip :type :ip}
   :state      {:title "State" :key :state :type :string}
   :dataset    {:title "Dataset" :type :string
                :key (partial api/get-sub-element :datasets :dataset
                              #(str (:name %) "-" (:version %)))}
   :package    {:title "Package" :type :string
                :key (partial api/get-sub-element :packages :package :name)}
   :owner      {:title "Owner" :type :string
                :key (partial api/get-sub-element :orgs :owner :name)}
   :hypervisor {:title "Hypervisor" :type :string
                :key (partial api/get-sub-element :hypervisors :hypervisor :alias)}))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "vmlistc")
    om/IWillMount
    (will-mount [this]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (vms/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (om/build view/render data {})))))
