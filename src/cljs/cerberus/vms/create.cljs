(ns cerberus.vms.create
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.grid :as g]
   [om-bootstrap.nav :as n]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.input :as i]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [cerberus.networks.api :as networks]
   [cerberus.ipranges.api :as ipranges]
   [cerberus.datasets.api :as datasets]
   [cerberus.packages.api :as packages]
   [cerberus.groupings.api :as groupings]
   [cerberus.create :as create]
   [clojure.string :refer [split]]
   [cerberus.state :refer [app-state]]
   [cerberus.utils :refer [make-event val-by-id ->state]]))


(def spec-alias {:label "Alias" :id "vm-alias" :key [:config :alias]})

(def spec-hostname {:label "Hostname" :id "vm-hostname" :key [:config  :hostname]
                    :validator #(re-matches #"[a-zA-Z]+[.a-zA-Z0-9-]*" (str %2))})

(def spec-dataset {:lable "Dataset" :key :dataset :validator #(not (empty? %2))})

(def spec-package {:label "Package" :key :package :validator #(not (empty? %2))})

(defn valid-networks [data networks]
  (if networks
    (let [dataset-uuid (get-in data [:data :dataset])]
      (if-let [required-networks (get-in data [:datasets :elements dataset-uuid :networks])]
        (every? #(networks (:name  %)) required-networks)
        false))
    false))

(def spec-networks {:label "Networks" :key [:config :networks] :validator valid-networks})

(def spec-general [spec-alias spec-hostname])

(def spec
  [spec-alias spec-hostname
   spec-dataset
   spec-package
   spec-networks])

(defn mkopts [data tab spec]
  {:key tab :on-click (make-event #(om/transact! data :key (constantly tab)))
   :class (if (create/validate-data data spec) "success" "danger")})

(defn invalid-resolvers? [resolvers]
  (re-matches #"(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?:,(?:[0-9]{1,3}\.){3}[0-9]{1,3})+" resolvers))

(defn main-tab [data]
  (d/form
   {:class "form-horizontal"}
   (create/input data spec spec-alias)
   (create/input data spec spec-hostname)))

(defn validate-data! [data]
  (create/validate-data! data spec))

(defn dataset-tab [data]
  (table
   {}
   (d/thead
    (d/tr
     (d/td "Name") (d/td "Type") (d/td "Version")))
   (d/tbody
    (map (fn [{:keys [uuid name type version]}]
           (d/tr
            {:class (if (= (get-in data [:data :dataset]) uuid) "active" "inactive")
             :on-click (make-event (fn []
                                     (om/transact! data [:data] #(assoc % :dataset uuid))
                                     (validate-data! (assoc-in data [:data :dataset] uuid))))}
            (d/td name) (d/td type) (d/td version)))
         (sort-by #(str (:name %) "-" (:version %))
                  (vals (get-in data [:datasets :elements])))))))

(defn package-tab [data]
  (table
   {}
   (d/thead
    (d/tr
     (d/td "Name") (d/td "CPU") (d/td "RAM") (d/td "Quota")))
   (d/tbody
    (map (fn [{:keys [uuid name cpu_cap ram quota]}]
           (d/tr
            {:class (if (= (get-in data [:data :package]) uuid) "active" "")
             :on-click (make-event (fn []
                                     (om/transact! data [:data] #(assoc % :package uuid))
                                     (validate-data! (assoc-in data [:data :package] uuid))))}
            (d/td name) (d/td cpu_cap) (d/td ram) (d/td quota)))
         (sort-by :name (vals (get-in data [:packages :elements])))))))

;; This kind of sucks, it tries to get a full row ...
(defn networking-tab [data owner state]
  (g/grid
   {:md 10}
   (g/row
    {}
    (let [dataset-uuid (get-in data [:data :dataset])
          dataset (get-in data [:datasets :elements dataset-uuid])
          networks (get-in data [:datasets :elements dataset-uuid :networks])
          cnt (count networks)]
      (map
       (fn [{nic :name :keys [description]}]
         (g/col
          {:md (Math/floor (/ 10 cnt))}
          (table
           {}
           (d/thead
            (d/tr
             (d/td (str nic ": " (name description)))))
           (d/tbody
            (map (fn [{:keys [uuid name ipranges]}]
                   (let [free (reduce + (map #(count (:free (get-in data [:ipranges :elements %]))) ipranges))]
                     (d/tr
                      {:class (if (= (get-in data [:data :config :networks nic]) uuid) "active" "")
                       :on-click (if (> free 0)
                                   (make-event (fn []
                                                 (om/transact! data [:data :config :networks] #(assoc % nic uuid))
                                                 (validate-data! (assoc-in data [:data :config :networks nic] uuid)))))}
                      (d/td name (if (= free 0) [" " (r/label {:bs-style "danger"} "No free IPs")])))))
                 (sort-by :name (vals (get-in data [:networks :elements]))))))))
       networks)))
   (g/row
    {}
    (g/col
     {:sm 8}
     (i/input {:type "text" :value (:resolvers state) :placeholder "8.8.8.8,8.8.4.4"
               :on-change (->state owner :resolvers)}))
    (g/col
     {:sm 2}
     (let [rs (split (:resolvers state) #",")
           current (get-in data [:data :config :resolvers])
           unchanged? (= current rs)]
       (b/button
        {:bs-style (if unchanged?  "success"  "primary")
         :on-click #(om/update! data [:data :config :resolvers] rs)
         :disabled? (or unchanged?
                        (empty? (:resolvers state))
                        (invalid-resolvers? (:resolvers state)))}
        "Set Resolvers"))))))

(defn advanced-tab [data owner state]
  (let [groupings (get-in  data [:groupings :elements])]
    (g/grid
     {:md 10}
     (g/row
      {}
      (g/col
       {:xs 1}
       (d/h4 "Cluster:"))
      (g/col
       {:xs 11}
       (i/input {:type "select" :value (get-in data [:data :config :grouping])
                 :id "vm-create-cluster"
                 :on-change
                 (make-event (fn [e]
                               (let [v (val-by-id "vm-create-cluster")]
                                 (if (empty? v)
                                   (om/update! data [:data :config] #(dissoc % :grouping))
                                   (om/update! data [:data :config :grouping] v)))))}
                (d/option "")
                (map #(d/option {:value (:uuid %)}  (:name %)) (filter #(= (:type %) "cluster") (map second groupings))))))
     (g/row
      {}
      (g/col
       {:xs 12}
       (d/h4 "Metadata")))
     (g/row
      {}
      (g/col
       {:sm 4}
       (i/input {:type "text" :value (:meta-name state) :placeholder "Name"
                 :on-change (->state owner :meta-name)}))
      (g/col
       {:sm 4}
       (i/input {:type "text" :value (:meta-val state) :placeholder "Value"
                 :on-change (->state owner :meta-val)}))
      (g/col
       {:sm 2}
       (b/button
        {:bs-style "primary"
         :on-click #(om/update! data [:data :config :metadata (:meta-name state)] (:meta-val state))
         :disabled? (or (empty? (:meta-name state)) (empty? (:meta-val state)))}
        "Add")))
     (g/row
      {}
      (g/col
       {:sm 10}
       (table
        {:condensed? true}
        (d/thead
         (d/tr
          (d/th "Key")
          (d/th "Value")))
        (d/tbody
         (map (fn [[r v]]
                (d/tr
                 (d/td r)
                 (d/td v)))
              (get-in data [:data :config :metadata])))))))))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "createvmc")
    om/IWillMount
    (will-mount [_]
      (networks/list data)
      (ipranges/list data)
      (groupings/list data)
      (datasets/list data)
      (packages/list data))
    om/IRenderState
    (render-state [_ state]
      (let [tab (get-in data [:key] 1)
            mkopts (partial mkopts data)
            ]
        (g/grid
         {:id "vm-create-grid" :class "vms-create"}
         (g/row
          {}
          (g/col
           {:md 2}
           (n/nav {:bs-style "stacked" :active-key tab :id "vm-create-tabs"}
                  (n/nav-item (mkopts 1 spec-general) "General")
                  (n/nav-item (mkopts 2 [spec-dataset]) "Dataset")
                  (n/nav-item (mkopts 3 [spec-package]) "Package")
                  (n/nav-item (mkopts 4 [spec-networks]) "Networking")
                  (n/nav-item (mkopts 5 []) "Advanced")))
          (g/col
           {:md 10}
           (condp = tab
             1 (main-tab data)
             2 (dataset-tab data)
             3 (package-tab data)
             4 (networking-tab data owner state)
             5 (advanced-tab data owner state)))))))))
