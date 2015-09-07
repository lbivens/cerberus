(ns cerberus.vms.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.howl :as howl]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state! update-state! app-state delete-state!]]))

(def root :vms)

(def list-fields
  "alias,uuid,config,state,dataset,package,metadata,dataset,hypervisor")

(defn list [data]
  (api/list data root list-fields))

(defn get [uuid]
  (howl/join uuid)
  (api/get root uuid))

(defn metrics [uuid]
  (api/to-state [root :elements uuid :metrics]
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
  (api/post
   root [uuid :snapshots]
   {:comment comment}))

(defn delete-snapshot [uuid snapshot]
  (api/delete root [uuid :snapshots snapshot]))

(defn restore-snapshot [uuid snapshot]
  (api/put root [uuid :snapshots snapshot] {:action "rollback"}
           #(get uuid) []))

(defn _backup [uuid opts]
  (api/post root [uuid :backups]
             opts
             (fn [resp]
               (if (:success resp)
                 (set-state! [root :elements uuid] (:body resp))))))

(defn add-fw-rule [uuid rule]
  (api/post root [uuid :fw_rules] rule
            #(get uuid)))

(defn delete-fw-rule [uuid rule]
  (api/delete
   root [uuid :fw_rules rule]
   #(get uuid)))

(defn backup
  ([uuid comment]
   (_backup uuid {:comment comment}))
  ([uuid parent comment]
   (_backup uuid {:comment comment :parent parent})))

(defn delete-backup [uuid backup]
  (api/delete root [uuid :backups backup]
              (fn [resp]
                (if (:success resp)
                  (delete-state! [root :elements uuid :backups backup])))))

(defn restore-backup [uuid backup]
  (api/put root [uuid :backups backup] {:action "rollback"}
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
  (api/delete root [uuid :nics mac] #(get uuid)))

(defn make-network-primary [uuid mac]
  (api/put root [uuid :nics mac] {:primary true} #(get uuid) []))

(def update-metadata (partial api/update-metadata root))
