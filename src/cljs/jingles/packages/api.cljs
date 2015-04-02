(ns jingles.packages.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.state :refer [set-state!]]))

(def root :packages)

(defn list [data]
  (api/list data root))

(def get (partial api/get root))

(def delete (partial api/delete root))
