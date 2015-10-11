(ns cerberus.datasets.create
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [cljs-http.client :as http]
   [cerberus.debug :as dbg]
   [cerberus.global :as global]
   [cerberus.fields :as fields]
   [cerberus.state :refer [app-state set-state! update-state!]]
   [cerberus.create :as create]
   [cerberus.datasets.api :as datasets :refer [root]]))


(defn submit [section data]
  (doall
   (map datasets/import data)))

(defn toggle-dataset [e {datasets :data valid :valid
                         :or {datasets #{} valid false} :as add}]
  (let [datasets (if (datasets e)
                   (disj datasets e)
                   (conj datasets e))
        valid (not (empty? datasets))]
    (assoc add :data datasets :valid valid)))


(defn type-name [type]
  (condp = type
    "lx-dataset" "lx"
    "zone-dataset" "zone"
    "zvol" "kvm"
    type))

(defn render [data]
  (reify
    om/IDisplayName
    (display-name [_]
      "addnetworkc")
    om/IWillMount
    (will-mount [_]
      (datasets/list data)
      (go
        (let [resp (<! (http/get (global/get "datasets" "http://datasets.at/images") {:with-credentials? false :headers {"Accept" "datalication/json"}}))]
          (if (:success resp)
            (om/transact! data :remote-datasets (constantly (:body resp)))
            (dbg/error "[datasets/import] error: " resp)))))
    om/IRenderState
    (render-state [_ datasets]
      (let [datasets   (:remote-datasets data)
            installed? (or  (get-in data [:datasets :elements]) {})
            picked?    (or (:data data) #{})]
        (pr installed?)
        (table
         {:striped? true :condensed? true
          :hover? true :responsive? true :id "remote-datasets"}
         (d/thead
          (d/td "Name")
          (d/td "Version")
          (d/td "Published")
          (d/td "Size"))
         (d/tbody
          (map
           (fn [{uuid :uuid :as e}]
             (d/tr
              {:on-click #(om/transact! data (partial toggle-dataset uuid))
               :class  (if (installed? uuid) "installed"
                           (if (picked? uuid) "selected" "not-selected"))}
              (d/td (:name e) " (" (type-name (:type e)) ")")
              (d/td (:version e))
              (d/td (:published_at e))
              (d/td (fields/fmt-bytes :b (get-in e [:files 0 :size])))))
           datasets)))))))
