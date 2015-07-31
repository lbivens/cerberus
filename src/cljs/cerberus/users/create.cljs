(ns cerberus.users.create
  (:require
   [om.core :as om :include-macros true]
   [cerberus.create :as create]))

(defn render [data]
  (reify
    om/IDisplayName
    (display-name [_]
      "adduserc")
    om/IRenderState
    (render-state [_ _]
      (create/render
       data
       {:type :input :label "Name" :id "user-name" :key :user}
       {:type :input :label "Password" :id "user-pass" :key :password}))))
