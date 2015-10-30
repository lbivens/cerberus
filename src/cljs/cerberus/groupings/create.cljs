(ns cerberus.groupings.create
  (:require
   [om.core :as om :include-macros true]
   [cerberus.create :as create]))

(defn render [app]
  (reify
    om/IDisplayName
    (display-name [_]
      "groupingadd")
    om/IRenderState
    (render-state [_ _]
      (create/render
       app
       {:type :select :label "Type" :id "grouping-type" :key :type
        :options [["Cluster" "cluster"] ["Stack" "stack"]] :default "cluster"}
       {:type :input :label "Name" :id "grouping-name" :key :name :validator #(not (empty? %))}))))
