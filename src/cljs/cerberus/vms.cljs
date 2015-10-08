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
   [om-bootstrap.modal :as md]
   [om-tools.dom :as d :include-macros true]

   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.list :as jlist]
   [cerberus.vms.view :as view]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.fields :refer [mk-config]]
   [cerberus.state :refer [set-state!]]))

(def token-path "sessions/one_time_token")

(defn open-with-ott [path]
  (go
    (let [response (<! (http/get token-path))]
      (if (= 200 (:status response))
        (let [ott (get-in response [:body :token])]
          (.open js/window (str path "&ott=" ott)))))))


(defn actions [e]
  (let [uuid (:uuid e)
        locked (get-in e [:raw :metadata :cerberus :locked] false)
        set-lock (partial vms/update-metadata uuid [:cerberus :locked])
        delete #(vms/delete uuid)
        hypervisor (get-in e [:raw :hypervisor])
        state (get-in e [:raw :state])]
    (if (or (not hypervisor) (empty? hypervisor))
      []
      [["Console" #(open-with-ott (str "./console.html?uuid=" uuid))]
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
       ["Delete" {:class (if locked "disabled")} #(set-state! [:delete] uuid)]])))

(defn get-ip [vm]
  (:ip (first (filter (fn [{p :primary}] p) (get-in vm [:config :networks])))))

(def state-map
  {"running" "success"
   "stopped" "default"
   "faiiled" "danger"})

(defn map-state [state]
  (let [style (or (state-map state) "default")]
    (r/label {:bs-style style} state)))

(def config
  (mk-config
   root "Machines" actions
   :name       {:title "Name" :key [:config :alias] :order -20}
   :hostname   {:title "Hostname" :key [:config :hostname] :order -18 :show false}
   :ip         {:title "IP" :key get-ip :type :ip :order -16}
   :package    {:title "Package" :type :string :order -14
                :key (partial api/get-sub-element :packages :package :name)}
   :dataset    {:title "Dataset" :type :string :order -12
                :key (partial api/get-sub-element :datasets :dataset
                              #(str (:name %) "-" (:version %)))}
   :owner      {:title "Owner" :type :string :order -10
                :key (partial api/get-sub-element :orgs :owner :name)}
   :cpu        {:title "CPU" :key [:config :cpu_cap] :type :percent :show false}
   :ram        {:title "Memory" :key [:config :ram] :type [:bytes :mb] :show false}
   :state      {:title "State" :key :state :type :string  :render-fn map-state}
   :hypervisor {:title "Hypervisor" :type :string :show false
                :key (partial api/get-sub-element :hypervisors :hypervisor :alias)}))

(set-state! [root :fields] (initial-state config))

(defn delete-modal [data]
  (let [id (:delete data)
        vm (get-in data [root :elements id])]
    (d/div
     {:style {:display (if id "block" "none")} }
     (md/modal
      {:header (d/h4
                "Delete VM"
                (d/button {:type         "button"
                           :class        "close"
                           :aria-hidden  true
                           :on-click #(set-state! [:delete] nil)}
                          "×"))
       :close-button? false
       :visible? true
       :animate? false
       :style {:display "block"}
       :footer (d/div
                (b/button {:bs-style "danger"
                           :disabled? false
                           :on-click #(do
                                        (vms/delete id)
                                        (set-state! [:delete] nil))}
                          "delete"))}
      "Are you sure that you want to delete the VM " (d/strong (get-in vm [:config :alias])) " (" id ")?" ))))
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
    (render-state [_ state]
      (condp = (:view data)
        :list (d/div
               {}
               (delete-modal data)
               (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
