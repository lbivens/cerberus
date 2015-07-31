(ns cerberus.datasets.create
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [cljs-http.client :as http]
   [cerberus.state :refer [app-state set-state! update-state!]]
   [cerberus.create :as create]
   [cerberus.datasets.api :as datasets :refer [root]]))


(defn submit [section data]
  (pr section data)
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
    om/IRenderState
    (render-state [_ _]
      (let [datasets (:remote-datasets data)
            installed? (get-in data [:datasets :elements] {})
            picked? (or (:data data) #{})]
        (if (empty installed?)
          (datasets/list data))
        (if (not datasets)
          (go
            (let [resp (<! (http/get "http://datasets.at/images" {:with-credentials? false :headers {"Accept" "datalication/json"}}))]
              (if (:success resp)
                (om/transact! data :remote-datasets (constantly (:body resp)))
                (pr "error: " resp)))))
        (table
         {:striped? true :condensed? true :hover? true :responsive? true :id "remote-datasets"}
         (d/thead
          (d/td "Name")
          (d/td "Version"))
         (d/tbody
          (map
           (fn [{uuid :uuid :as e}]
             (d/tr
              {:on-click #(om/transact! data (partial toggle-dataset uuid))
               :class  (if (installed? uuid) "installed" (if (picked? uuid) "selected" "not-selected"))}
              (d/td (:name e) " (" (type-name (:type e)) ")")
              (d/td (:version e)))) datasets)))))))
