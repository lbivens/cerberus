(ns jingles.networks.create
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
                                        ;[jingles.networks.api :refer [root]]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.config :as conf]
   [jingles.create :as create]
   [jingles.utils :refer [initial-state make-event val-by-id]]
   [jingles.state :refer [set-state!]]))


(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "network-name" :key :name :validator #(not (empty? %))}))
