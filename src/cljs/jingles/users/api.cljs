(ns jingles.users.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state!]]))

(def root :users)

(def list-fields
  "uuid,name")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(def delete (partial api/delete root))

(defn changepass [uuid newpass]
  (api/put root [uuid] {:password newpass})
  (println "PASSWORD SET. TODO Alert"))