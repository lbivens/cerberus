(ns cerberus.scopes
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.datasets.api :refer [root] :as datasets]
   [cerberus.fields :refer [mk-config]]
   [om-bootstrap.random :as r]
   [cerberus.datasets.view :as view]
   [cerberus.utils :refer [initial-state]]
   [cerberus.state :refer [set-state!]]))

(defn actions []
  [])

(def config (mk-config root "Datasets" actions
                       :version {:title "Version" :key :version :type :string}
                       :imported {:title "Imported" :key :imported :type :percent-flt}))

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
