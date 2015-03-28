(ns jingles.packages
  (:refer-clojure :exclude [get list])
  (:require
            [jingles.list :as jlist]
            [jingles.packages.api :refer [root] :as api]
            [om-bootstrap.random :as r]
            [jingles.utils :refer [initial-state]]
            [jingles.state :refer [set-state!]]
            [jingles.fields :refer [mk-config]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(api/delete uuid)]])

(def config (mk-config root "Packages" actions
                       :cpu_cap {:title "CPU" :key :cpu_cap :type :percent}
                       :quota {:title "Quota" :key :quota :type [:bytes :gb]}
                       :ram {:title "RAM" :key :ram :type [:bytes :mb]}))

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
