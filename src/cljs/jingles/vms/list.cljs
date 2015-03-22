(ns jingles.vms.list
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [jingles.api :as api]
            [om.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [jingles.list :as jlist]
            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state app-alerts set-alerts! set-state!]]
            [om-bootstrap.input :as i]))

(set-state! [:vm :all-fields] [{:title "Name" :key '(:config :alias)}
                               {:title "UUID" :key :uuid}])

(set-state! [:vm :fields] (get-in @app-state [:vm :all-fields]))

(def list-fields
  "alias,uuid,config")

(defn full-list []
  (api/to-state [:vm :list] (api/full-list "vms" list-fields)))

(defn render [app]
  (jlist/view "Machines" :vm app))
