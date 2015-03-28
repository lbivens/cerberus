(ns jingles.hypervisors
  (:refer-clojure :exclude [get list])
  (:require
   [jingles.list :as jlist]
   [jingles.hypervisors.api :refer [root] :as api]
   [om-bootstrap.random :as r]
   [jingles.fields :refer [mk-config]]
   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(api/delete uuid)]])

(def config (mk-config root "Hypervisors" actions
                       :name {:title "Name" :key :alias :order -20}))

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
