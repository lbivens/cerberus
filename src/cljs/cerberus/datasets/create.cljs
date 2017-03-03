(ns cerberus.datasets.create
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.random :as r]
   [cljs-http.client :as http]
   [cemerick.url :as url]
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


(defn dataset-type [{type :type}]
  (condp = type
    "zone-dataset" "Zone"
    "lx-dataset"   "LX"
    "zvol"         "KVM"
    type))

(defn render [data]
  (reify
    om/IDisplayName
    (display-name [_]
      "addnetworkc")
    om/IWillMount
    (will-mount [_]
      (datasets/list data)

      (doseq
          ;; This kind of sucks, but we are changing the config data type,
          ;; so at least with this something shows up, and it will be relatively
          ;; easy to debug why the users config changes are not being used.
          [f (if (coll? (global/get "datasets" ["https://datasets.project-fifo.net/images/", "http://datasets.at/images"]))
               (global/get "datasets" ["https://datasets.project-fifo.net/images/", "http://datasets.at/images"])
               ["https://datasets.project-fifo.net/images/", "http://datasets.at/images"])]
        (go (let
                [resp (<! (http/get f {:with-credentials? false
                                       :headers {"Accept" "application/json"}}))]

              (if (:success resp)
                (om/transact! data [:remote-datasets f] (constantly (:body resp)))
                (dbg/error "[datasets/import] error: " resp))))))



    om/IRenderState
    (render-state [_ datasets]
      (let [datasets   (:remote-datasets data)
            installed? (or  (get-in data [:datasets :elements]) {})
            picked?    (or (:data data) #{})]
        (table
         {:striped? true :condensed? true
          :hover? true :responsive? true :id "remote-datasets"}
         (d/thead
          (d/td "Name")
          (d/td "Version")
          (d/td "Type")
          (d/td "Published")
          (d/td "Source")
          (d/td "Size"))
         (d/tbody
          (for [source datasets]
            (for [dataset (second source)]
              (let [uuid   (:uuid dataset)
                    source (first source)]
                (d/tr
                 {:on-click #(om/transact! data (partial toggle-dataset (str source "/" uuid)))
                  :class  (if (installed? uuid) "installed"
                              (if (picked? (str source "/" uuid)) "selected" "not-selected"))}
                 (d/td (:name dataset))
                 (d/td (:version dataset))
                 (d/td (dataset-type dataset))
                 (d/td (:published_at dataset))
                 (d/td
                  (if (= "https" (:protocol (url/url source)))
                    (r/glyphicon {:glyph "lock"}))
                  (:host (url/url source)))
                 (d/td (fields/fmt-bytes :b (get-in dataset [:files 0 :size])))))))))))))
