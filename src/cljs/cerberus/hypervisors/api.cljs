(ns cerberus.hypervisors.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :hypervisors)

(def list-fields
  "uuid,alias")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid] (alerts "Hypervisor removed." "Failed to remove hypervisor.")))

(defn rename [uuid name]
  (api/put root [uuid :config] {:alias name} (a-get uuid "Hypervisor renamed." "Failed to rename hypervisor.")))

(defn service-action [uuid service action]
  (api/put root [uuid :services] {:service service :action action}
           (a-get uuid "Service state changed." "Failed to change service state.")))

(defn metrics [uuid]
  (api/to-state [root :elements uuid :metrics]
                (http/get [root uuid :metrics])))

