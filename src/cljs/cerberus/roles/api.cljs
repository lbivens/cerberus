(ns cerberus.roles.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.state :refer [set-state!]]))

(def root :roles)

(def list-fields
  "uuid,name")

(defn list [data]
  (pr "list")
  (api/list data root list-fields))

(def get (partial api/get root))

(defn delete [uuid]
  (api/delete root [uuid]))


(defn grant [uuid perm]
  (api/put root (concat [uuid :permissions] perm) {}
           #(get uuid) []))

(defn revoke [uuid perm]
  (api/delete root (concat [uuid :permissions] perm)
           #(get uuid)))
