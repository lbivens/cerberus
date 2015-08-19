(ns cerberus.users.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :refer [join]]
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state! delete-state!]]))

(def root :users)

(def list-fields
  "uuid,name")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn delete [uuid]
  (api/delete root [uuid]))

(defn changepass [uuid newpass]
  (api/put root [uuid] {:password newpass})
  (println "PASSWORD SET. TODO Alert"))

(defn grant [uuid perm]
  (api/put root (concat [uuid :permissions] perm) {}
           #(get uuid) []))

(defn revoke [uuid perm]
  (api/delete root (concat [uuid :permissions] perm)
           #(get uuid)))

(defn addkey [uuid keyname keydata]
  (api/put root [uuid :keys] {keyname keydata} api/get [root uuid])
  (println "SSH KEY ADDED. TODO Alert"))

(defn deletekey [uuid keyname]
  (api/delete root (concat [uuid :keys keyname])
              #(get uuid)))

(defn add-role [uuid role]
  (api/put root [uuid :roles role] {}  #(get uuid) []))

(defn remove-role [uuid role]
  (api/delete root [uuid :roles role] #(get uuid)))

(defn add-org [uuid role]
  (api/put root [uuid :orgs role] {}  #(get uuid) []))

(defn active-org [uuid role]
  (api/put root [uuid :orgs role] {:active true}  #(get uuid) []))

(defn remove-org [uuid role]
  (api/delete root [uuid :orgs role] #(get uuid)))
