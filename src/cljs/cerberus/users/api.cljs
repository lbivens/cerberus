(ns cerberus.users.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :refer [join]]
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.alert :refer [alerts]]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state! delete-state!]]))

(def root :users)

(def list-fields
  "uuid,name,org")

(defn list [data]
  (api/list data root list-fields))

(defn get [uuid]
  (api/get root uuid))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid]
              (alerts "User deletion successful." "Failed to delete User.")))

(defn changepass [uuid newpass]
  (api/put root [uuid] {:password newpass}
           (alerts "Password changed." "Failed to change password.")))

(defn grant [uuid perm]
  (api/put root (concat [uuid :permissions] perm) {}
           (a-get uuid "Permission granted." "Failed to grant permission.")))

(defn revoke [uuid perm]
  (api/delete root (concat [uuid :permissions] perm)
              (a-get uuid "Permission revoed." "Failed to revoke permission.")))

(defn revoke-token [uuid token]
  (api/delete root [uuid :tokens token]
              (a-get uuid "Token revoked." "Failed to revoke token.")))

(defn add-sshkey [uuid keyname keydata]
  (api/put root [uuid :keys] {keyname keydata}
           (a-get uuid "SSH key added." "Failed to add SSH key.")))

(defn add-yubikey [uuid keyid]
  (api/put root [uuid :yubikeys] {:otp keyid}
           (a-get uuid "Yubikey added." "Failed to add Yubikey.")))

(defn delete-sshkey [uuid key-name]
  (api/delete root (concat [uuid :keys key-name])
              (a-get uuid "SSH key removed." "Failed to remove SSH key.")))

(defn delete-yubikey [uuid key-id]
  (api/delete root (concat [uuid :yubikeys key-id])
              (a-get uuid "Yubikey removed." "Failed to remove Yubikey.")))

(defn add-role [uuid role]
  (api/put root [uuid :roles role] {}
           (a-get uuid "Role added." "Failed to add role.")))

(defn remove-role [uuid role]
  (api/delete root [uuid :roles role]
              (a-get uuid "Role removed." "Failed to remve role.")))

(defn add-org [uuid org]
  (api/put root [uuid :orgs org] {}
           (a-get uuid "Organisation joined." "Failed to join organisation.")))

(defn active-org [uuid role]
  (api/put root [uuid :orgs role] {:active true}
           (a-get uuid "Organisation set as active." "Failed to set organisation as active.")))

(defn remove-org [uuid role]
  (api/delete root [uuid :orgs role]
              (a-get uuid "Organisation left." "Failed to leave organisation.")))
