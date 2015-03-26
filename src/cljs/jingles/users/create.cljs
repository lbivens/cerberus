(ns jingles.users.create
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   ;[jingles.users.api :refer [root]]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.create :as create]
   [jingles.state :refer [set-state!]]))

(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "user-name" :key :user :validator #(not (empty? %))}
   {:type :input :label "Password" :id "user-pass" :key :password :validator #(not (empty? %))}))
