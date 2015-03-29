(ns jingles.datasets.create
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [cljs-http.client :as http]
   [jingles.config :as conf]
   [jingles.state :refer [app-state set-state! update-state!]]
   [jingles.create :as create]
   [jingles.datasets.api :as api :refer [root]]))


(defn submit [section app]
  (doall
   (map api/import (conf/get [:add :data]))))

(defn toggle-dataset [e {datasets :data valid :valid
                         :or {datasets #{} valid false} :as add}]
  (let [datasets (if (datasets e)
                   (disj datasets e)
                   (conj datasets e))
        valid (not (empty? datasets))]
    (pr add)
    (assoc add :data datasets :valid valid)))

(defn render [app]
  (let [datasets (:remote-datasets app)
        installed? (get-in app [root :elements])
        known? (get-in app [:config :add :data] #{})]
    (if (empty? datasets)
      (go
        (let [resp (<! (http/get "http://datasets.at/images" {:with-credentials? false :headers {"Accept" "application/json"}}))]
          (if (:success resp)
            (set-state! :remote-datasets (:body resp))
            (pr "error: " resp)))))
    (table
     {:striped? true :bordered? true :condensed? true :hover? true :responsive? true :id "remote-datasets"}
     (d/thead
      (d/td "Name")
      (d/td "Version"))
     (d/tbody
      (map
       (fn [{uuid :uuid :as e}]
         (d/tr
          {:on-click #(conf/update! [:add] (partial toggle-dataset uuid))
           :class  (if (installed? uuid) "installed" (if (known? uuid) "selected" "not-selected"))}
          (d/td (:name e))
          (d/td (:version e)))) datasets)))))
