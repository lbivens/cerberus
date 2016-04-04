(ns cerberus.vms
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [cerberus.vms.api :refer [root]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [cerberus.vms.api :as vms]
   [cerberus.orgs.api :as orgs]
   [cerberus.datasets.api :as datasets]
   [cerberus.users.api :as users]
   [cerberus.del :as del]
   [cerberus.packages.api :as packages]
   [cerberus.hypervisors.api :as hypervisors]

   [om-tools.dom :as d :include-macros true]

   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.list :as jlist]
   [cerberus.vms.view :as view :refer [open-with-ott]]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.fields :refer [mk-config]]
   [cerberus.state :refer [set-state!]]))

(defn actions [e]
  (let [uuid (:uuid e)
        locked (get-in e [:raw :metadata :cerberus :locked] false)
        set-lock (partial vms/update-metadata uuid [:cerberus :locked])
        hypervisor (get-in e [:raw :hypervisor])
        type (get-in e [:raw :config :type])
        state (get-in e [:raw :state])]
    (if (or (not hypervisor) (empty? hypervisor))
      [["Delete" {:class (if locked "disabled")} #(del/show uuid)]]
      [["Console" #(open-with-ott (str "./" (if (= type "kvm") "vnc" "console")  ".html?uuid=" uuid))]
       (if locked
         ["Unlock" #(set-lock false)]
         ["Lock" #(set-lock true)])
       :divider
       (if (= state "running")
         ["Stop" {:class (if locked "disabled")} #(vms/stop uuid)]
         ["Start" {:class (if locked "disabled")} #(vms/start uuid)])
       (if (= state "running")
         ["Reboot" {:class (if locked "disabled")} #(vms/reboot uuid)])
       :divider
       ["Delete" {:class (if locked "disabled")} #(del/show uuid)]])))

(defn get-ip [vm]
  (:ip (first (filter (fn [{p :primary}] p) (get-in vm [:config :networks])))))

(def state-map
  {"running" "success"
   "stopped" "default"
   "failed" "danger"})

(defn map-state [state]
  (let [style (or (state-map state) "default")]
    (r/label {:bs-style style} state)))

(defn brand [config]
  (let [brand (get-in config [:config :type])
        type  (get-in config [:config :zone_type])]
    (if (empty? type)
      brand
      type)))

(def config
  (mk-config
   root "Machines" actions
   :name       {:title "Name" :key [:config :alias] :order -20}
   :hostname   {:title "Hostname" :key [:config :hostname] :order -18 :show false}
   :ip         {:title "IP" :key get-ip :type :ip :order -16}
   :created_at  {:title "Created" :type [:timstamp :s] :order -15
                 :key :created_at :show false}
   :created_ago  {:title "Created ago" :type [:ago :s] :order -14
                  :key :created_at :show true
                  :sort-key #(or (:created_at %) 0)}
   :package    {:title "Package" :type :string :order -13
                :key (partial api/get-sub-element :packages :package :name)}
   :dataset    {:title "Dataset" :type :string :order -12
                :key (fn [vm]
                       (if (= (:vm_type vm) "docker")
                         (:dataset vm)
                         (api/get-sub-element :datasets :dataset
                                              #(str (:name %) " (" (:version %) ")")
                                              vm)))}
   :owner      {:title "Owner" :type :string :order -10
                :key (partial api/get-sub-element :orgs :owner :name)}
   :created_by {:title "Creator" :order -5
                :key (partial api/get-sub-element :users :created_by :name) :show false}
   :cpu        {:title "CPU" :key [:config :cpu_cap] :type :percent :show false}
   :ram        {:title "Memory" :key [:config :ram] :type [:bytes :mb] :show false}
   :brand      {:title "Brand" :key brand :type :string :show false}
   :state      {:title "State" :key :state :type :string  :render-fn map-state}
   :hypervisor {:title "Hypervisor" :type :string :show false
                :key (partial api/get-sub-element :hypervisors :hypervisor :alias)}
   :cluster    {:title "Cluster" :type :string :show false
                :key (partial api/get-sub-element :groupings #(first (:groupings %)) :name)}))

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
      (vms/list data)
      (orgs/list data)
      (packages/list data)
      (datasets/list data)
      (users/list data)
      (hypervisors/list data))
    om/IRenderState
    (render-state [_ state]
      (condp = (:view data)
        :list (del/with-delete
                data root #(get-in % [:config :alias]) vms/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
