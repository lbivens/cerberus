(ns cerberus.datasets.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.state :refer [set-state!]]))

(def root :datasets)

(def server "http://datasets.at/images")

(defn list [data]
  (api/list data root))

(def get (partial api/get root))

(defn delete [uuid]
  (api/delete root [uuid]))

(defn import [uuid]
  (api/post root [] {"url" (str server "/" uuid)}))
