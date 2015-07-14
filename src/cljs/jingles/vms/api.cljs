(ns jingles.vms.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state! update-state! app-state delete-state!]]))

(def root :vms)

(def list-fields
  "alias,uuid,config,state,dataset,package,metadata")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn metrics [uuid]
  (api/to-state [:metrics]
                (http/get [root uuid :metrics])))

(defn delete [uuid]
  (api/delete root [uuid]))

(defn start [uuid]
  (api/put root [uuid :state] {:action :start}))

(defn stop [uuid & [force]]
  (api/put root [uuid :state]
           (if force
             {:action :stop :force true}
             {:action :stop})))


(defn reboot [uuid & [force]]
  (api/put root [uuid :state]
           (if force
             {:action :reboot :force true}
             {:action :reboot})))

(defn snapshot [uuid comment]
  (api/post root [uuid :snapshots]
            {:comment comment}
            (fn [resp]
              (if (:success resp)
                (let [snapshot (:body resp)]
                  (update-state! [root :elements uuid :snapshots] assoc (:uuid snapshot) snapshot))))))

(defn delete-snapshot [uuid snapshot]
  (api/delete root [uuid :snapshots snapshot]
              (fn [resp]
                (if (:success resp)
                  (delete-state! [root :elements uuid :snapshots snapshot])))))

(defn restore-snapshot [uuid snapshot]
  (api/put root [uuid :snapshots snapshot] {:action "rollback"}
           #(get uuid) []))

(defn service-action [uuid service action]
  (api/put root [uuid :services] {:service service :action action}
           #(get uuid) []))

(defn change-package [uuid package]
  (api/put root [uuid :package] {:package package}
           #(get uuid) []))

(defn add-network [uuid network]
  (api/post root [uuid :nics] {:network network}
            #(get uuid)))

(defn delete-network [uuid mac]
  (api/delete root [uuid :nics mac]
              (fn [resp]
                (if (:success resp)
                  (delete-state! [root :elements uuid :networks mac])))))

(def update-metadata (partial api/update-metadata root))
