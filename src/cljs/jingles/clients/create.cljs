(ns jingles.clients.create
  (:require
   [om.core :as om :include-macros true]
   [jingles.create :as create]))

(defn render [data]
  (reify
    om/IDisplayName
    (display-name [_]
      "addclientc")
    om/IRenderState
    (render-state [_ _]
      (create/render
       data
       {:type :input :label "Name" :id "client-name" :key :client}
       {:type :input :label "Secret" :id "client-pass" :key :secret}))))
