(ns jingles.ipranges.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.state :refer [set-state!]]))

(def root :ipranges)

(def list-fields
  "uuid,name")

(defn list []
  (api/list root list-fields))

(def get (partial api/get root))