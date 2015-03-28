(ns jingles.users
  (:refer-clojure :exclude [get list])
  (:require
   [om-bootstrap.random :as r]
   [jingles.list :as jlist]
   [jingles.fields :refer [mk-config]]
   [jingles.utils :refer [initial-state]]
   [jingles.state :refer [set-state!]]
   [jingles.users.api :refer [root] :as api]))


(defn actions [{uuid :uuid}]
  [["Delete" #(api/delete uuid)]])

(def config (mk-config root "Users" actions))

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
