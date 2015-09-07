(ns cerberus.ipranges.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :ipranges)


(defn list [data]
  (api/list data root))

(def get (partial api/get root))

(defn delete [uuid]
  (api/delete root [uuid] (alerts "IPRange deleted." "Failed to delete IPRange.")))
