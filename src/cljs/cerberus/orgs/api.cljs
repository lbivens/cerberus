(ns cerberus.orgs.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :orgs)

(def list-fields
  "name,uuid")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid] (alerts "Organisation deleted." "Failed to delete organisation.")))

(defn delete-trigger [uuid trigger]
  (api/delete
   root [uuid :triggers trigger]
   (a-get uuid "Trigger deleted." "Failed to delete trigger.")))

(defn add-trigger [uuid trigger payload]
  (api/post
   root [uuid :triggers trigger] payload
   (a-get uuid "Trigger added." "Failed to add trigger.")))
