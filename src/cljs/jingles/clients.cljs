(ns jingles.clients
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [jingles.list :as jlist]
   [jingles.clients.api :refer [root] :as clients]
   [om-bootstrap.random :as r]
   [jingles.clients.view :as view]
   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(clients/delete uuid)]])

(def config (mk-config root "Clients" actions))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "clientviewc")
    om/IWillMount
    (will-mount [_]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (clients/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (om/build view/render data {})))))
