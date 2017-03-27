(ns cerberus.dtrace.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :dtrace)

(def list-fields
  "uuid,name")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn get-page [uuid]
  (api/get-page root uuid))

(defn delete [data uuid]
  (api/delete data root [uuid] (alerts "DTrace script deleted." "Failed to delete DTrace SCript")))
