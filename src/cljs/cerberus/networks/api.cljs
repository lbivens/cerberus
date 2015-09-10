(ns cerberus.networks.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :networks)

(def list-fields
  "uuid,name")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid] (alerts "Network deleted." "Failed to delete network.")))

(defn add-iprange [uuid iprange]
  (api/put root [uuid :ipranges iprange] {}
           (a-get uuid "IPRange added." "Failed to add IPRange.")))

(defn remove-iprange [uuid iprange]
  (api/delete root [uuid :ipranges iprange]
              (a-get uuid "IPRange removed." "Failed to remove IPRange.")))
