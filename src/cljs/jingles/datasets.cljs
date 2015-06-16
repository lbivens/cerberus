(ns jingles.datasets
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [jingles.list :as jlist]
   [jingles.datasets.api :refer [root] :as datasets]
   [jingles.fields :refer [mk-config]]
   [om-bootstrap.random :as r]
   [jingles.datasets.view :as view]
   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(datasets/delete uuid)]])

(def config (mk-config root "Datasets" actions
                        :version {:title "Version" :key :version :type :string}))

(set-state! [root :fields] (initial-state config))

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
      (datasets/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (om/build view/render data {})))))
