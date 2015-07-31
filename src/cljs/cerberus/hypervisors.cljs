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

(def config (mk-config root "Hypervisors" actions
                       :name {:title "Name" :key :alias :order -20}))

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
