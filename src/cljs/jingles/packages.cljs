(ns jingles.packages
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [om-bootstrap.random :as r]
   [jingles.list :as jlist]
   [jingles.packages.api :refer [root] :as packages]
   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(packages/delete uuid)]])

(def config (mk-config root "Packages" actions
                       :cpu_cap {:title "CPU" :key :cpu_cap :type :percent}
                       :quota {:title "Quota" :key :quota :type [:bytes :gb]}
                       :ram {:title "RAM" :key :ram :type [:bytes :mb]}))

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
      "datasetviewc")
    om/IWillMount
    (will-mount [_]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (packages/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (show-view data)))))
