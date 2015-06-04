(ns jingles.datasets.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.state :refer [set-state!]]))

(def root :datasets)

(def server "http://datasets.at/images")

(defn list [data]
  (api/list data root))

(def get (partial api/get root))

(def delete (partial api/delete root))

(defn import [uuid]
  (pr "impoting" uuid)
  (api/post root [] {"url" (str server "/" uuid)}))
