(ns cerberus.groupings.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :groupings)

(def list-fields
  "uuid,name,groupings,elements,type")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid] (alerts "Grouping removed." "Failed to remove grouping.")))

(defn set-config [uuid conf val]
  (api/put root [uuid :config] {conf val}
           (a-get uuid "Configuration updated." "Failed to update configuration.")))

(defn delete-config [uuid conf]
  (api/delete root [uuid :config conf]
              (a-get uuid "Configuration deleted." "Failed to delete configuration.")))

(defn add-element [uuid grouping]
  (api/put root [uuid :elements grouping] {}
           (a-get uuid "Grouping added." "Failed to add grouping.")))

(defn remove-element [uuid grouping]
  (api/delete root [uuid :elements grouping]
              (a-get uuid "Grouping removed." "Failed to remve grouping.")))
