(ns jingles.hypervisors.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state!]]))

(def root :hypervisors)

(def list-fields
  "uuid,alias")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(def delete (partial api/delete root))

(defn rename [uuid name]
  (api/put root [uuid :config] {:alias name} api/get [root uuid]))