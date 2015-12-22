(ns cerberus.hypervisors
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.hypervisors.api :refer [root] :as hypervisors]
   [om-bootstrap.random :as r]
   [cerberus.hypervisors.view :as view]
   [cerberus.fields :refer [mk-config]]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(hypervisors/delete uuid)]])

(def config
  (mk-config
   root "Hypervisors" actions
   :name {:title "Name" :key :alias :order -20}
   :version {:title "Version" :key :version :order 1}
   :os-version {:title "OS Version" :key [:sysinfo (keyword "Live Image")] :order 2}
   :last-seen {:title "Last seen" :key :last_seen
               :type [:ago :s] :order 3}
   :used-men {:title "Used Memory" :key [:resources :provisioned-memory]
              :type [:bytes :mb] :order 3}
   :reserved-men {:title "Reserved Memory" :key [:resources :reserved-memory]
                  :type [:bytes :mb] :order 4}
   :free-men {:title "Free Memory" :key [:resources :free-memory]
              :type [:bytes :mb]  :order 5}))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "hypervisorlistc")
    om/IWillMount
    (will-mount [this]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (hypervisors/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (om/build view/render data {})))))
