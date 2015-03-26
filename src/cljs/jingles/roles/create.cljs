(ns jingles.roles.create
  (:require
   [jingles.create :as create]))

(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "role-name" :key :name :validator #(not (empty? %))}))
