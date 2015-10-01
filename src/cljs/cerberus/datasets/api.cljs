(ns cerberus.datasets.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.api :as api]
   [cerberus.global :as global]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :datasets)

(def server (global/get "datasets" "http://datasets.at/images"))

(defn list [data]
  (api/list data root))

(def get (partial api/get root))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [uuid]
  (api/delete root [uuid]
              (alerts "Dataset deleted." "Failed to delete dataset.")))

(defn import [uuid]
  (api/post root [] {:url (str server "/" uuid)}
            (alerts "Dataset import started." "Dataset import failed.")))

(defn add-nic [uuid nic desc]
  (api/put root [uuid :networks nic] {:description desc}
           (a-get uuid "Dataset network added." "Failed to add dataset network.")))

(defn delete-nic [uuid nic]
  (api/delete root [uuid :networks nic]
              (a-get uuid "Dataset network added." "Failed to add dataset network.")))
