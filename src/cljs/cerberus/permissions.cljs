(ns cerberus.permissions
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.button :as b]
   [om-bootstrap.grid :as g]
   [om-bootstrap.input :as i]
   [om-bootstrap.random :as r]
   [om-bootstrap.table :refer [table]]

   [cerberus.vms.api :as vms]
   [cerberus.datasets.api :as datasets]
   [cerberus.hypervisors.api :as hypervisors]

   [cerberus.packages.api :as packages]
   [cerberus.networks.api :as networks]
   [cerberus.ipranges.api :as ipranges]
   [cerberus.dtrace.api :as dtrace]
   [cerberus.users.api :as users]
   [cerberus.roles.api :as roles]
   [cerberus.orgs.api :as orgs]
   [cerberus.clients.api :as clients]

   [cerberus.utils :refer [row ->state make-event menu-items]]
   [cerberus.state :refer [app-state]]))


(defn cloud_perms [title]
  {:title title :children {"..." {:title  "Everything"} "list" {:title  "List"}}})

(defn cloud_perms_c [title]
  (assoc-in (cloud_perms title) [:children "create" :title] "Create"))


(defn elements [data root n sub-perms]
  (reduce
   (fn [acc [id v]] (assoc acc id {:title [(n v) " ("(d/span {:class "uuid"} id) ")"] :children sub-perms}))
   {"..." {:title  "Everything" :pos 0} "_" {:title  "Any" :pos 1  :children sub-perms}}
   (get-in data  [root :elements])))


(def base-perms
  {"_" {:title "Everything" :pos 0}
   "get" {:title "See"}
   "delete" {:title "Delete"}
   "edit" {:title "Edit"}})

(def dataset-perms
  (assoc base-perms "create" {:title "Create"}))

(def dtrace-perms
  (assoc base-perms
         "stream" {:title "Stream"}))

(def perm-perms
  (assoc base-perms
         "granta" {:title "Grant a Permission"}
         "revoke" {:title "Revoke a Permission"}))

(def role-perms
  (assoc perm-perms
         "join"   {:title "Join a role"},
         "leave"  {:title "Leave a role"}))

(def org-perms
  (assoc perm-perms
         "join"   {:title "Join a role"}))

(def user-perms
  (assoc perm-perms
         "join"   {:title "Join a role"},
         "leave", {:title "Leave a role"}))

(def hv-perms
  (assoc base-perms
         "create" {:title "Create VM's here"}))


(def vm-perms
  (assoc base-perms
         "state"           {:title "State"}
         "console"         {:title "Console/VNC"}
         "snapshot"        {:title "Create a Snapshot"}
         "rollback"        {:title "Rollback a Snapshot"}
         "snapshot_delete" {:title "Delete a Snapshot"}))

(defn perms [data]
  {"..."      {:title  "Everything" :pos 0}
   "channels" {:title "Channels"}
   "cloud"    {:title "Cloud" :pos 1
               :children {"..." {:title  "Everything"}
                          "cloud" {:title "Cloud" :children {"..." {:title  "Everything"} "status" {:title  "Status"}}}
                          "datasets" (assoc-in (cloud_perms "Datasets") [:children "import" :title] "Import")
                          "dtraces" (cloud_perms_c "DTrace")
                          "roles" (cloud_perms_c "Roles")
                          "groupings" (cloud_perms_c "Groupings")
                          "hypervisors" (cloud_perms "Hyervisors")
                          "ipranges" (cloud_perms_c "IP Ranges")
                          "networks" (cloud_perms_c "Networks")
                          "orgs" (cloud_perms_c "Organizations")
                          "packages" (cloud_perms_c "Packages")
                          "users" (cloud_perms_c "Users")
                          "clients" (cloud_perms_c "Clients")
                          "vms" (cloud_perms_c "Virtual Machines")}}
   "datasets"    {:title "Datasets"         :children (elements data :datasets    :name  dataset-perms)}
   "dtraces"     {:title "DTrace"           :children (elements data :dtrace      :name  dtrace-perms)}
   "groupings"   {:title "Groupings"        :children (elements data :groupings   :name  base-perms)}
   "hypervisors" {:title "Hypervisors"      :children (elements data :hypervisors :alias hv-perms)}
   "ipranges"    {:title "IP Ranges"        :children (elements data :ipranges    :name  base-perms)}
   "networks"    {:title "Networks"         :children (elements data :networks    :name  base-perms)}
   "orgs"        {:title "Organizations"    :children (elements data :orgs        :name  org-perms)}
   "packages"    {:title "Packages"         :children (elements data :packages    :name  base-perms)}
   "roles"       {:title "Roles"            :children (elements data :roles       :name  role-perms)}
   "users"       {:title "Users"            :children (elements data :users       :name  user-perms)}
   "vms"         {:title "Virtual Machines" :children (elements data :vms         #(get-in % [:config :alias]) vm-perms)}})

(defn highlight [part]
  (condp = part
    "_" (d/b "_")
    "..." (d/b "...")
    part))


(defn options [l]
  (map
   (fn [[k v]]
     (d/option {:value k} (:title v)))
   (sort-by (fn [e] [(get-in e [:pos] 100) (:title e)]) l)))

(defn end [{l1v :l1 l2v :l2 l3v :l3 :as state} data]
  (if (and (not (empty? l1v)) (nil? (get-in data [l1v :children])))
    [l1v]
    (if (and (not (empty? l2v))
             (nil? (get-in data [l1v :children l2v :children])))
      [l1v l2v]
      (if (and (not (empty? l3v))
               (nil? (get-in data [l1v :children l2v :children l3v :children])))
        [l1v l2v l3v]
        nil))))

(defn perm [revoke p]
  (d/tr
   (d/td (butlast (interleave (map highlight p) (repeat "->"))))
   (d/td {:on-click #(revoke p)} (r/glyphicon {:glyph "trash"}))))

(defn render [data owner {element :element grant :grant revoke :revoke :or {grant pr revoke pr}}]
  (reify
    om/IDisplayName
    (display-name [_]
      "rules-well")
    om/IInitState
    (init-state [_]
      (vms/list data)
      (datasets/list data)
      (hypervisors/list data)
      (packages/list data)
      (networks/list data)
      (ipranges/list data)
      (dtrace/list data)
      (users/list data)
      (roles/list data)
      (orgs/list data)
      (clients/list data)
      {})
    om/IRenderState
    (render-state [_ {l1v :l1 l2v :l2 l3v :l3 :as state}]
      (let [perms (perms data)
            uuid (:uuid element)
            grant (partial grant uuid)
            revoke (partial revoke uuid)]
        (r/well
         {}
         (row
          (g/col
           {:xs 3}
           (i/input
            {:type "select"
             :default-value nil
             :on-change (->state owner :l1)}
            (d/option  {:value nil} "")
            (options perms)))
          (if-let [l1 (get-in perms [l1v :children])]
            [(g/col
              {:xs 3}
              (i/input
               {:type "select"
                :default-value ""
                :on-change (->state owner :l2)}
               (d/option  "")
               (options l1)))
             (if-let [l2 (get-in l1 [l2v :children])]
               (g/col
                {:xs 3}
                (i/input
                 {:type "select"
                  :default-value ""
                  :on-change (->state owner :l3)}
                 (d/option  "")
                 (options l2))))])
          (g/col
           {:xs 3}
           (if-let [path (end state perms)]
             (b/button {:bs-style "primary" :on-click #(grant path)} "Grant"))))
         (row
          (g/col
           {:xs 12}
           (d/table
            {:striped? true :condensed? true :hover? true :responsive? true
             :style {:width "100%"}}
            (d/thead
             {:striped? false}
             (d/tr
              {}
              (d/td)
              (d/td)))
            (d/tbody
             {}
             (map (partial perm  revoke) (:permissions element)))))))))))
