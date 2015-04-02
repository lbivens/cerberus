(ns jingles.networks.create
  (:require
   [om.core :as om :include-macros true]
   [jingles.create :as create]))


(defn render [app]
  (reify
    om/IDisplayName
    (display-name [_]
      "addnetworkc")
    om/IRenderState
    (render-state [_ _]
      (create/render
       app
       {:type :input :label "Name" :id "network-name" :key :name :validator #(not (empty? %))}))))
