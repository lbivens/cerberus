(ns jingles.hypervisors
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [jingles.list :as jlist]
   [jingles.hypervisors.api :refer [root] :as hypervisors]
   [om-bootstrap.random :as r]
   [jingles.hypervisors.view :as view]
   [jingles.fields :refer [mk-config]]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(hypervisors/delete uuid)]])

(def config (mk-config root "Hypervisors" actions
                       :name {:title "Name" :key :alias :order -20}))

(set-state! [root :fields] (initial-state config))

(defn show-view [app]
  (let [uuid (get-in app [root :selected])
        element (get-in app [root :elements uuid])]
    (r/well
     {}
     (pr-str element))))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify 
    om/IDisplayName
    (display-name [_]
      "hypervisrolistc")
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
