(ns cerberus.roles.create
  (:require
   [om.core :as om :include-macros true]
   [cerberus.create :as create]))

(defn render [app]
  (reify
    om/IDisplayName
    (display-name [_]
      "addrolec")
    om/IRenderState
    (render-state [_ _]
      (create/render
       app
       {:type :input :label "Name" :id "role-name" :key :name}))))
