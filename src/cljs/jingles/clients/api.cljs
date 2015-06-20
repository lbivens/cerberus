(ns jingles.clients.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.state :refer [set-state!]]))

(def root :clients)

(def list-fields
  "name,uuid")

(defn list [data]
  (api/list data root list-fields))

(def get (partial api/get root))

(def delete (partial api/delete root))

(defn grant [uuid perm]
  (api/put root (concat [uuid :permissions] perm) nil
           #(get uuid) []))

(defn revoke [uuid perm]
  (api/delete root (concat [uuid :permissions] perm)
           #(get uuid)))
