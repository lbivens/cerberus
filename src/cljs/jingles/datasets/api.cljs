(ns jingles.datasets.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.state :refer [set-state!]]))

(def root :datasets)
(def server "http://datasets.at/images")

(def list-fields
  "name,uuid,version")

(def list (partial api/list root list-fields))

(def get (partial api/get root))

(def delete (partial api/delete root))

(defn import [uuid]
  (api/post root [] {"url" (str server "/" uuid)}))
