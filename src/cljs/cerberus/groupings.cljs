(ns cerberus.groupings
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [om-bootstrap.random :as r]
   [om-tools.dom :as d :include-macros true]
   [cerberus.del :as del]

   [cerberus.list :as jlist]
   [cerberus.groupings.api :refer [root] :as groupings]
   [cerberus.groupings.view :as view]
   [cerberus.fields :refer [mk-config]]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [(del/menue-item uuid)])

(def config
  (mk-config
   root "Groupings" actions
   :name {:title "Name" :key :name :order -20}
   :type {:title "Type" :key :type :order 0}
   :elements {:title "Elements" :key #(+ (count (:elements %))
                                         (count (:groupings %))) :order 10}))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "groupinglistc")
    om/IWillMount
    (will-mount [this]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (groupings/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (del/with-delete
                data root :name groupings/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
