(ns jingles.networks.create
  (:require
   [jingles.create :as create]))


(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "network-name" :key :name :validator #(not (empty? %))}))
