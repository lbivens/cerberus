(ns jingles.servers.list
  (:require [jingles.api :as api]
            [jingles.list :as jlist]
            [jingles.state :refer [set-state!]]))

(def root :server)

(def config {:fields {:name {:title "Name" :key :alias}
                      :uuid {:title "UUID" :key :uuid}}
             :root root
             :title "Servers"})

(set-state! [root :fields] (keys (:fields config)))

(def list-fields
  "uuid,alias")

(defn full-list []
  (api/to-state [root :list] (api/full-list "hypervisors" list-fields)))

(defn render [app]
  (jlist/view config app))
