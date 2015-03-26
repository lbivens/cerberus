(ns jingles.roles
  (:refer-clojure :exclude [get list])
  (:require [jingles.api :as api]
            [jingles.roles.api :refer [root]]
            [jingles.utils :refer [initial-state]]
            [om-bootstrap.random :as r]
            [jingles.list :as jlist]
            [jingles.state :refer [set-state!]]))

(def config {:fields {:name {:id :name :title "Name" :key :name}
                      :uuid {:id :uuid :title "UUID" :key :uuid}}
             :root root
             :title "Roles"})

(set-state! [root :fields] (initial-state config))

(defn show-view [app]
  (let [uuid (get-in app [root :selected])
        element (get-in app [root :elements uuid])]
    (r/well
     {}
     (pr-str element))))

(defn render [app]
  (condp = (:view app)
    :list (jlist/view config app)
    :show (show-view app)))
