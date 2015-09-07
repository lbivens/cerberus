(ns cerberus.clients.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :clients)

(def list-fields
  "name,uuid")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn a-get [uuid success error]
  (merge (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid] (alerts "Client deleted." "Failed to delete client.")))

(defn grant [uuid perm]
  (api/put root (concat [uuid :permissions] perm) {}
           (a-get uuid "Permission granted." "Failed to grant permission.")))

(defn revoke [uuid perm]
  (api/delete root (concat [uuid :permissions] perm)
              (a-get uuid "Permission revoked." "Failed to revoke permission.")))
