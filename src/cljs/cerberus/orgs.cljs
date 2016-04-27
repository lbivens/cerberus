(ns cerberus.orgs
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [om-bootstrap.random :as r]
   [om-tools.dom :as d :include-macros true]
   [cerberus.del :as del]
   [cerberus.list :as jlist]
   [cerberus.orgs.api :refer [root] :as orgs]
   [cerberus.orgs.view :as view]

   [cerberus.utils :refer [initial-state]]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [(del/menue-item uuid)])

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
        :list (del/with-delete
                data root :name orgs/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
