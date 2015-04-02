(ns jingles.hypervisors
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [jingles.list :as jlist]
   [jingles.hypervisors.api :refer [root] :as hypervisors]
   [om-bootstrap.random :as r]
   [jingles.fields :refer [mk-config]]
   [jingles.utils :refer [initial-state]]
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

(defn render [data owner opts]
  (reify 
    om/IDisplayName
    (display-name [_]
      "hypervisrolistc")
    om/IWillMount
    (will-mount [_]
      (hypervisors/list data)
      (om/transact! data [root :filter] (constantly ""))
      (om/transact! data [root :filted] (constantly []))
      (om/transact! data [root :fields] (jingles.utils/initial-state config))
      (pr "mouuuunt!"))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view (root data) {:opts {:config config}})
        :show (show-view data)))))
