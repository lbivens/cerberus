(ns jingles.datasets.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.state :refer [set-state!]]))

(def root :datasets)

(def list-fields
  "name,uuid,version")

(def list (partial api/list root list-fields))

(def get (partial api/get root))
