(ns cerberus.vms.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.howl :as howl]
   [cerberus.alert :as alert :refer [alerts]]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state! update-state! app-state delete-state!]]))

(def root :vms)


(def list-fields
  "alias,uuid,config,state,dataset,package,metadata,dataset,hypervisor,owner")

(defn list [data]
  (api/list data root list-fields))

(defn get [uuid]
  (howl/join uuid)
  (api/get root uuid))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(api/get root uuid)))

(defn metrics [uuid]
  (api/to-state [root :elements uuid :metrics]
                (http/get [root uuid :metrics])))

(defn delete [uuid]
  (api/delete
   root [uuid]
   (alerts "VM Deletion successful." "Failed to delete VM.")))

(defn delete-hypervisor [uuid]
  (api/delete
   root [uuid :hypervisor]
   (a-get uuid "VM successfuly removed from hypervisor."
          "Failed to remove VM from hypervisor.")))

(defn start [uuid]
  (api/put root [uuid :state] {:action :start}
           (alerts "Starting VM." "Failed to start VM.")))

(defn stop [uuid & [force]]
  (api/put root [uuid :state]
           (if force
             {:action :stop :force true}
             {:action :stop})
           (alerts "Stopping VM." "Failed to stop VM.")))


(defn reboot [uuid & [force]]
  (api/put root [uuid :state]
           (if force
             {:action :reboot :force true}
             {:action :reboot})
           (alerts "Rebooting VM." "Failed to reboot VM.")))

(defn snapshot [uuid comment]
  (api/post
   root [uuid :snapshots]
   {:comment comment}
   (alerts "Creating Snapshot." "Failed to create snapshot.")))

(defn delete-snapshot [uuid snapshot]
  (api/delete root [uuid :snapshots snapshot]
              (alerts "Deleting Snapshot." "Failed to delete snapshot.")))

(defn restore-snapshot
  ([uuid snapshot]
   (api/put root [uuid :snapshots snapshot] {:action "rollback"}
            (a-get uuid "Restoring Snapshot." "Failed to restore snapshot."))))

(defn _backup [uuid opts]
  (api/post root [uuid :backups]
            opts
            (assoc (alerts "Creating backup." "Failed to create backup.")
                   :always
                   (fn [resp]
                     (if (:success resp)
                       (set-state! [root :elements uuid] (:body resp)))))))

(defn add-fw-rule [uuid rule]
  (api/post root [uuid :fw_rules] rule
            (a-get uuid "Adding firewall rule." "Failed to add firewall rule.")))

(defn delete-fw-rule [uuid rule]
  (api/delete
   root [uuid :fw_rules rule]
   (a-get uuid "Deleting firewall rule." "Failed to delete firewall rule.")))

(defn backup
  ([uuid comment]
   (_backup uuid {:comment comment}))
  ([uuid parent comment]
   (_backup uuid {:comment comment :parent parent})))

(defn delete-backup [uuid backup]
  (api/delete root [uuid :backups backup]
              (assoc (alerts "Deleting backup." "Failed to delete backup.")
                     :always
                     (fn [resp]
                       (if (:success resp)
                         (delete-state! [root :elements uuid :backups backup]))))))

(defn restore-backup
  ([uuid backup]
   (api/put root [uuid :backups backup] {:action "rollback"}
            (a-get uuid "Restoring backup." "Failed to restore backup.")))
  ([uuid hypervisor backup]
   (api/put root [uuid :backups backup] {:action "rollback" :hypervisor hypervisor}
            (a-get uuid "Restoring backup." "Failed to restore backup."))))

(defn service-action [uuid service action]
  (api/put root [uuid :services] {:service service :action action}
           (a-get uuid "Changing service state." "Failed to change service state.")))

(defn change-package [uuid package]
  (api/put root [uuid :package] {:package package}
           (a-get uuid "Changing VM package." "Failed to change VM package.")))

(defn add-network [uuid network]
  (api/post root [uuid :nics] {:network network}
            (a-get uuid "Adding network." "Failed to add network.")))

(defn delete-network [uuid mac]
  (api/delete root [uuid :nics mac]
              (a-get uuid "Deleting network." "Failed to add network.")))

(defn make-network-primary [uuid mac]
  (api/put root [uuid :nics mac] {:primary true}
           (a-get uuid "Marking network as primary." "Failed to mark network as primary.")))

(def update-metadata (partial api/update-metadata root))


(defn set-owner [uuid org]
  (api/put root [uuid :owner] {:org org}
           (a-get uuid "Owner changed." "Failed to change owner.")))
