(ns cerberus.networks
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [om-bootstrap.random :as r]
   [cerberus.del :as del]
   [om-tools.dom :as d :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.networks.view :as view]
   [cerberus.networks.api :refer [root] :as networks]
   [cerberus.utils :refer [initial-state]]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [(del/menue-item uuid)])

(def config
  (mk-config
   root "Networks" actions
   :ipranges {:name "IP Ranges" :key #(count (:ipranges %))
              :formater #(if (= 0 %)
                           (r/label {:bs-style "danger"} %)
                           %)}))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "networkviewc")
    om/IWillMount
    (will-mount [_]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (networks/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (del/with-delete
                data root :name networks/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
