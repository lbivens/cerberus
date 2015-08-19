(ns cerberus.hypervisors.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.utils :refer [initial-state make-event]]
   [cerberus.state :refer [set-state!]]))

(def root :hypervisors)

(def list-fields
  "uuid,alias")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn delete [uuid]
  (api/delete root [uuid]))

(defn rename [uuid name]
  (api/put root [uuid :config] {:alias name} api/get [root uuid]))

(defn service-action [uuid service action]
  (api/put root [uuid :services] {:service service :action action}
           #(get uuid) []))
