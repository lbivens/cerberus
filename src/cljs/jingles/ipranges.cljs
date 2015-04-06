(ns jingles.ipranges
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [jingles.list :as jlist]
   [jingles.ipranges.api :refer [root] :as ipranges]
   [om-bootstrap.random :as r]
   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [mk-config]]))


(defn actions [{uuid :uuid}]
  [["Delete" #(api/delete uuid)]])

(def config (mk-config root "IP Ranges" actions))

(set-state! [root :fields] (initial-state config))

(defn show-view [app]
  (let [uuid (get-in app [root :selected])
        element (get-in app [root :elements uuid])]
    (r/well
     {}
     (pr-str element))))

(defn render [data owner opts]
    (reify
    om/IDisplayName
    (display-name [_]
      "iprangeviewc")
    om/IWillMount
    (will-mount [_]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (ipranges/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (om/build jlist/view data {:opts {:config config}})
        :show (show-view data)))))
