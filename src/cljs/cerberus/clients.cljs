(ns cerberus.clients
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.clients.api :refer [root] :as clients]
   [om-bootstrap.random :as r]
   [cerberus.del :as del]
   [cerberus.clients.view :as view]
   [cerberus.utils :refer [initial-state]]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [(del/menue-item uuid)])

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
        :list (del/with-delete
                data root :name clients/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
