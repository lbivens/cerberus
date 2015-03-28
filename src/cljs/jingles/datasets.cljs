(ns jingles.datasets
  (:refer-clojure :exclude [get list])
  (:require [jingles.list :as jlist]
            [jingles.datasets.api :refer [root] :as api]
            [jingles.fields :refer [mk-config]]
            [om-bootstrap.random :as r]
            [jingles.utils :refer [initial-state]]
            [jingles.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(api/delete uuid)]])

(def config (mk-config root "Datasets" actions
                        :version {:title "Version" :key :version :type :string}))

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
