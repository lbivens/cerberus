(ns cerberus.networks.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.state :refer [set-state!]]))

(def root :networks)

(def list-fields
  "uuid,name")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(defn delete [uuid]
  (api/delete root [uuid]))

(defn add-iprange [uuid iprange]
  (api/put root [uuid :ipranges iprange] {}  #(get uuid) []))

(defn remove-iprange [uuid iprange]
  (api/delete root [uuid :ipranges iprange] #(get uuid)))
