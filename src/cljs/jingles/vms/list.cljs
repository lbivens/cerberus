(ns jingles.vms.list
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [jingles.http :as http]
            [om.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [jingles.list :as jlist]
            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state app-alerts set-alerts! set-state!]]
            [om-bootstrap.input :as i]))

(set-state! [:vm :all-fields] [{:title "uuid" :key :uuid}
                               {:title "alias" :key '(:config :alias)}])

(set-state! [:vm :fields] [{:title "Name" :key '(:config :alias)}
                           {:title "UUID" :key :uuid}])

(def list-fields
  "alias,uuid,config")
(defn full-list []
  (go (let [resp (<! (http/get "vms" {"x-full-list" "true"
                                      "x-full-list-fields" list-fields}))]
        (set-state! [:vm :list] (js->clj (:body resp))))))

(defn render [app]
  (jlist/view "Machines" :vm app))
