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
(defn get-page [uuid]
  (api/get-page root uuid))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [data uuid]
  (api/delete data root [uuid] (alerts "Organisation deleted." "Failed to delete organisation.")))

(defn delete-trigger [uuid trigger]
  (api/delete
   root [uuid :triggers trigger]
   (a-get uuid "Trigger deleted." "Failed to delete trigger.")))

(defn add-trigger [uuid trigger payload]
  (api/post
   root [uuid :triggers trigger] payload
   (a-get uuid "Trigger added." "Failed to add trigger.")))

(defn dec-resource [uuid res val]
  (api/put
   root [uuid :resources res] {:dec val}
   (a-get uuid "Decreased resource." "Failed to decreased resource.")))

(defn set-net [uuid scope net]
  (api/put
   root [uuid :docker :networks scope] {:network net}
   (a-get uuid "Network set." "Failed to set resource.")))

(defn inc-resource [uuid res val]
  (api/put
   root [uuid :resources res] {:inc val}
   (a-get uuid "Increase resource." "Failed to increase resource.")))

(defn delete-resource [uuid res]
  (api/delete
   root [uuid :resources res]
   (a-get uuid "Resource deleted." "Failed to delete resource.")))
