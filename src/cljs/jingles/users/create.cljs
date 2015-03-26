(ns jingles.users.create
  (:require
   [jingles.create :as create]))

(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "user-name" :key :user :validator #(not (empty? %))}
   {:type :input :label "Password" :id "user-pass" :key :password :validator #(not (empty? %))}))
