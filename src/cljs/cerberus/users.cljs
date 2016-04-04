(ns cerberus.users
  (:refer-clojure :exclude [get list])
  (:require
   [cerberus.api :as api]
   [cerberus.del :as del]
   [cerberus.fields :refer [mk-config]]
   [cerberus.list :as jlist]
   [cerberus.orgs.api :as orgs]
   [cerberus.state :refer [set-state!]]
   [cerberus.users.api :refer [root] :as users]
   [cerberus.users.view :as view]
   [cerberus.utils :refer [initial-state]]
   [om-bootstrap.random :as r]
   [om-tools.dom :as d :include-macros true]
   [om.core :as om :include-macros true]))


(defn actions [{uuid :uuid}]
  [(del/menue-item uuid)])

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
        :list (del/with-delete
                data root :name users/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
