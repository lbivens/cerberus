(ns jingles.orgs
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [jingles.list :as jlist]
   [jingles.orgs.api :refer [root] :as orgs]
   [om-bootstrap.random :as r]
   [jingles.orgs.view :as view]

   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(orgs/delete uuid)]])

(def config (mk-config root "Organisations" actions))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "orgviewc")
    om/IWillMount
    (will-mount [_]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (orgs/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (om/build view/render data {})))))
