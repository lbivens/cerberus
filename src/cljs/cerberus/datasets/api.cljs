(ns cerberus.datasets.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.api :as api]
   [cerberus.global :as global]
   [cerberus.alert :refer [alerts raise]]
   [cerberus.state :refer [set-state!]]))

(def root :datasets)

(defn list [data]
  (api/list data root))

(def get (partial api/get root))

(defn get-page [uuid]
  (api/get-page root uuid))

(defn a-get [uuid success error]
  (assoc (alerts success error) :always #(get uuid)))

(defn delete [data uuid]
  (api/delete data root [uuid]
              (alerts "Dataset deleted." "Failed to delete dataset.")))

(defn import [url]
  (let [success "Dataset import started."
        error "Dataset import failed."]
    (api/post root [] {:url url}
              {:success (fn [resp]
                          ((api/post-success-fn root) resp)
                          (raise :success success))
               :error #(raise :danger error)
               503 #(raise :danger  (str error " - not all services are available."))
               404 #(raise :warning (str error " - not found."))})))

(defn from-vm [vm snapshot name version os desc]
  (let [payload {:vm vm
                 :snapshot snapshot
                 :config {:name name
                          :version version
                          :os os
                          :description desc}}]
    (api/post root [] payload
              (alerts "Dataset import started." "Dataset import failed."))))

(defn add-nic [uuid nic desc]
  (api/put root [uuid :networks nic] {:description desc}
           (a-get uuid "Dataset network added." "Failed to add dataset network.")))

(defn delete-nic [uuid nic]
  (api/delete root [uuid :networks nic]
              (a-get uuid "Dataset network added." "Failed to add dataset network.")))
