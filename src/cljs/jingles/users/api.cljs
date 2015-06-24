(ns jingles.users.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :refer [join]]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state! delete-state!]]))

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

(defn addkey [uuid keyname keydata]
  (api/put root [uuid :keys] {keyname keydata} api/get [root uuid])
  (println "SSH KEY ADDED. TODO Alert"))

(defn deletekey [uuid keyname]
  (api/delete root (concat [uuid :keys keyname])
              #(get uuid)))

