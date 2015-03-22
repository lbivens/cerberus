(ns jingles.servers.list
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [cljs-http.client :as httpc]
            [jingles.http :as http]
            [om.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [jingles.list :as jlist]
            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state app-alerts set-alerts! set-state!]]
            [om-bootstrap.input :as i]))

(set-state! [:server :all-fields] [{:title "uuid" :key :uuid}
                                   {:title "name" :key :name}])

(set-state! [:server :fields] [{:title "Name" :key :alias}
                               {:title "UUID" :key :uuid}])

(defn full-list []
  (go (let [resp (<! (http/get "hypervisors" {"x-full-list" "true"}))]
        (set-state! [:server :list] (js->clj (:body resp))))))

(defn render [app]
  (jlist/view "Servers" :server app))
