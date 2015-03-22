(ns jingles.datasets.list
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [jingles.api :as api]
            [om.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [jingles.list :as jlist]
            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state app-alerts set-alerts! set-state!]]
            [om-bootstrap.input :as i]))

(set-state! [:dataset :all-fields] [{:title "Name" :key :name}
                                    {:title "Version" :key :version}
                                    {:title "UUID" :key :uuid}])

(set-state! [:dataset :fields] (get-in @app-state [:dataset :all-fields]))

(def list-fields
  "name,uuid,version")

(defn full-list []
  (api/to-state [:dataset :list] (api/full-list "datasets" list-fields)))

(defn render [app]
  (jlist/view "Datasets" :dataset app))
