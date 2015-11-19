(ns cerberus.users
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.users.api :refer [root] :as users]
   [cerberus.orgs.api :as orgs]
   [om-bootstrap.random :as r]
   [cerberus.users.view :as view]
   [cerberus.api :as api]
   [cerberus.fields :refer [mk-config]]
   [cerberus.utils :refer [initial-state]]
   [cerberus.state :refer [set-state!]]))


(defn actions [{uuid :uuid}]
  [["Delete" #(users/delete uuid)]])

(def config (mk-config
             root "Users" actions
             :name {:title "Name" :key :name :order -10}
             :org {:title "Organisation"
                   :key (partial api/get-sub-element :orgs :org :name)}))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "userviewc")
    om/IWillMount
    (will-mount [_]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (users/list data)
      (orgs/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (om/build view/render data {}))))) 
