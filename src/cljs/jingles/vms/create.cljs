(ns jingles.vms.create
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.grid :as g]
   [om-bootstrap.nav :as n]
   [om-bootstrap.table :refer [table]]
   [jingles.config :as conf]
   [jingles.networks.api :as networks]
   [jingles.datasets.api :as datasets]
   [jingles.packages.api :as packages]
   [jingles.create :as create]
   [jingles.state :refer [app-state set-state!]]
   [jingles.utils :refer [make-event val-by-id]]))


(defn opts [tab spec]
  {:key tab :on-click (make-event #(conf/write! [:add :key] tab))
   :class (if (create/validate-data spec) "success" "danger")})

(def spec-alias {:label "Alias" :id "vm-alias" :key [:config :alias]})

(def spec-hostname {:label "Hostname" :id "vm-hostname" :key [:config  :hostname]
                    :validator #(re-matches #"[a-z]+" (str %))})

(def spec-dataset {:lable "Dataset" :key :dataset :validator #(not (empty? %))})

(def spec-package {:label "Package" :key :package :validator #(not (empty? %))})

(defn valid-networks [networks]
  (if networks
    (if-let [required-networks (get-in @app-state [:datasets :elements (conf/get [:add :data :dataset]) :networks])]
      (every? #(networks (:name  %)) required-networks)
      false)
    false))

(def spec-networks {:label "Networks" :key [:config :networks] :validator valid-networks})

(def spec-general [spec-alias spec-hostname])

(def spec
  [spec-alias spec-hostname
   spec-dataset
   spec-package
   spec-networks
   ])

(defn and-validate [fun]
  (make-event
   (fn []
     (fun)
     (create/validate-data! spec))))

(defn render [app]
  (let [tab (conf/get [:add :key] 1)]
    (if (not (get-in app [:config :add :started]))
      (do
        (set-state! [:config :add :started] true)
        (networks/list)
        (datasets/list)
        (packages/list)))
    (g/grid
     {:id "vm-create-grid"}
     (g/row
      {}
      (g/col
       {:md 2}
       (n/nav {:bs-style "stacked" :active-key tab :id "vm-create-tabs"}
              (n/nav-item (opts 1 spec-general) "General")
              (n/nav-item (opts 2 [spec-dataset]) "Dataset")
              (n/nav-item (opts 3 [spec-package]) "Package")
              (n/nav-item (opts 4 [spec-networks]) "Networking")
              (n/nav-item (opts 5 []) "Advanced")
              ))
      (g/col
       {:md 10}
       (condp = tab
         1 (d/form
            {:class "form-horizontal"}
            (create/input spec spec-alias)
            (create/input spec spec-hostname)
            )
         2 (table
            {}
            (d/thead
             (d/tr
              (d/td "Name") (d/td "Version")))
            (d/tbody
             (map (fn [{:keys [uuid name version]}]
                    (d/tr
                     {:class (if (= (conf/get [:add :data :dataset]) uuid) "active" "inactive")
                      :on-click (make-event (fn []
                                              (conf/write! [:add :data :dataset] uuid)
                                              (create/validate-data! spec)))}
                     (d/td name) (d/td version)))
                  (sort-by #(str (:name %) "-" (:version %))
                           (vals (get-in app [:datasets :elements]))))))
         3 (table
            {}
            (d/thead
             (d/tr
              (d/td "Name") (d/td "CPU") (d/td "RAM") (d/td "Quota")))
            (d/tbody
             (map (fn [{:keys [uuid name cpu_cap ram quota]}]
                    (d/tr
                     {:class (if (= (conf/get [:add :data :package]) uuid) "active" "")
                      :on-click (make-event (fn []
                                              (conf/write! [:add :data :package] uuid)
                                              (create/validate-data! spec)))}
                     (d/td name) (d/td cpu_cap) (d/td ram) (d/td quota)))
                  (sort-by :name (vals (get-in app [:packages :elements]))))))
         ;; This kind of sucks, it tries to get a full row ...
         4 (g/grid
            {:md 10}
            (g/row
             {}
             (let [dataset-uuid (conf/get [:add :data :dataset])
                   dataset (get-in app [:datasets :elements dataset-uuid])
                   networks (get-in app [:datasets :elements dataset-uuid :networks])
                   cnt (count networks)]
               (pr dataset-uuid "->" dataset "->" networks)
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
                     (map (fn [{:keys [uuid name]}]
                            (d/tr
                             {:class (if (= (conf/get [:add :data :config :networks nic]) uuid) "active" "")
                              :on-click (make-event (fn []
                                                      (conf/write! [:add :data :config :networks nic] uuid)
                                                      (create/validate-data! spec)))}
                             (d/td name)))
                          (sort-by :name (vals (get-in app [:networks :elements]))))))))
                networks))))
         5 (d/div "Advanced")))))))
