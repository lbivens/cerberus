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

(defn get-page [uuid]
  (api/get-page root uuid))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [data uuid]
  (api/delete data root [uuid] (alerts "Client deleted." "Failed to delete client.")))

(defn grant [uuid perm]
  (api/put root (concat [uuid :permissions] perm) {}
           (a-get uuid "Permission granted." "Failed to grant permission.")))

(defn revoke [uuid perm]
  (api/delete root (concat [uuid :permissions] perm)
              (a-get uuid "Permission revoked." "Failed to revoke permission.")))

(defn change-secret [uuid secret]
  (api/put root [uuid] {:secret secret}
           (alerts "Secret changed." "Failed to change secret.")))

(defn add-uri [uuid uri]
  (api/post root [uuid :uris] {:uri uri}
           (a-get uuid "URI added." "Failed to add uri.")))

(defn delete-uri [uuid uri-id]
  (api/delete root [uuid :uris uri-id]
           (a-get uuid "URI added." "Failed to add uri.")))
