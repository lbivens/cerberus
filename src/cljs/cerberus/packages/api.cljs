(ns cerberus.packages.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [app-state set-state!]]))

(def root :packages)

(defn list [data]
  (api/list data root))

(def get (partial api/get root))
(defn get-page [uuid]
  (api/get-page root uuid))

(defn a-delete [uuid success error]
  (assoc (alerts success error) :success #(swap! app-state update-in [root :elements]
                                                 (fn [es] (dissoc es uuid)))))

(defn delete [data uuid]
  (api/delete data root [uuid] (a-delete uuid "Package deleted." "Failed to delete package.")))
