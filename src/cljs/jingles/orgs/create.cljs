(ns jingles.orgs.create
  (:require
   [jingles.create :as create]))

(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "org-name" :key :name :validator #(not (empty? %))}))
