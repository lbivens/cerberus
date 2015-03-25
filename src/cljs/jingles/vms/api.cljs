(ns jingles.vms.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state!]]))

(def root :vms)

(def list-fields
  "alias,uuid,config,state,dataset,package,metadata")

(def list (partial api/list root list-fields))

(def get (partial api/get root))

(defn change-package [uuid package]
  (api/request-and-get http/post root uuid [:package] {:package package}))
