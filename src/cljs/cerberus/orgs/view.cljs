(ns cerberus.orgs.view
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [lg li goto grid-row to-date ->state]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.users.api :as users]
   [cerberus.roles.api :as roles]
   [cerberus.orgs.api :refer [root] :as orgs]
   [cerberus.view :as view]
   [cerberus.permissions :as perms]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state! app-state]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))


(defn start-of-month []
  (let [date (js/Date.)]
         (.setUTCDate date 1)
         (.setUTCHours date 0)
         (.setUTCMinutes date 0)
         (.setUTCSeconds date 0)
         (* (.setUTCMilliseconds date 0) 1000)))

(defn now []
  (* (.getTime (js/Date.)) 1000))

(defn prepare-res [events]
  (map (fn [{t :timestamp a :action}] [t a])
       (sort-by :timestamp events))
  )
(defn render-resources [{uuid :uuid} owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {})
    om/IWillMount
    (will-mount [_]
      (go
        (let [start (start-of-month)
              end   (now)
              resp (<! (http/get [root uuid (str "accounting?start=" start "&end="end)]))]
          (if (:success resp)
            (om/set-state! owner :acc (:body resp))))))
    om/IRenderState
    (render-state [_ state]
      (r/well
       {}
       (g/row
        {}
        (map
         (fn [[res data]]
           (g/col
            {:xs 12 :sm 6 :md 4}
            (p/panel
             {:header (d/h3 res)
              :list-group
                (d/ul
                 {:class "list-group"}
                 (map li (prepare-res data)))})))
         (group-by :resource (:acc state)))

        )))))


(defn mk-trigger [{actor      :actor
                   action     :action
                   target     :target
                   permission :permission}]
  (if (and target (not (empty? target)))
    (condp = actor
      "vm_create"
      {:action action
       :target target
       :base "vms"
       :permission [permission]}
      "user_create"
      (cond
        (#{"join_org" "join_role"} action)
        {:action action
         :target target}
        (#{"grant_user" "grant_role"} action)
        {:action action
         :target target
         :base "users"
         :permission [permission]}
        :else nil)
      "dataset_create"
      {:action action
       :target target
       :base "datasets"
       :permission [permission]}
      nil)))

(def trigger-name
  {"vm_create" "When a VM is created"
   "user_create" "When an User is created"
   "dataset_create" "When a Dataset is created"})


(defn get-role [data uuid]
  (get-in data [roles/root :elements uuid :name]))

(defn get-org [data uuid]
  (get-in data [roles/root :elements uuid :name]))

(defn role-to-text [data trigger]
  (match
   [trigger]
   [{:action "role_grant" :target role :permission perm}]
   (d/span "Grant the role " (d/strong (get-role data role)) ": "
           (d/strong (get-in perms/vm-perms [(last perm) :title])))
   [{:action "user_grant" :target user :permission perm}]
   (d/span "Grant the user " (d/strong user))
   [{:action "join_org" :target org}]
   (d/span "Join the Organisation " (d/strong (get-in data [root :elements org :name])))
   [{:action "join_role" :target role}]
   (d/span "Receive the role " (d/strong (get-role data role)))
   [_] (pr-str trigger)))

(defn render-triggers [data owner {:keys [id]}]
  (reify
    om/IInitState
    (init-state [_]
      {:actor "vm_create" :action "role_grant"
       :permission "_"})
    om/IRenderState
    (render-state [_ state]
      (let [triggers (get-in data [root :elements id :triggers])
            actor (:actor state)
            action (:action state)
            target (:target state)
            permission (:permission state)]
        (r/well
         {}
         (g/row
          {}
          (g/col
           {}
           (d/div
            {:id "trigger-wizzard"}
            "When "
            (i/input
             {:type "select" :on-change (->state owner :actor)
              :value actor :id "actor-select"}
             (d/option {:value "vm_create"}      "a VM")
             (d/option {:value "user_create"}    "an User")
             (d/option {:value "dataset_create"} "a Dataset"))
            " is created "
            (i/input
             {:type "select" :value action :id "action-select"
              :on-change (->state owner :action) :valid? false}
             (if (= "user_create" actor) (d/option {:value "join_org"} "join them to the organization"))
             (if (= "user_create" actor) (d/option {:value "join_role"} "give them the role"))
             (d/option {:value "role_grant"} "grant the role")
             (d/option {:value "user_grant"} "grant the user"))
            (cond
              (#{"join_role" "role_grant"} action)
              (i/input {:type "select" :value target :id "target-select"
                        :on-change (->state owner :target)}
                       (d/option)
                       (map
                        (fn [[uuid role]]
                          (d/option {:value uuid} (:name role)))
                        (get-in data [:roles :elements])))
              (= action "join_org")
              (i/input {:type "select" :value target
                        :on-change (->state owner :target)}
                       (d/option)
                       (map
                        (fn [[uuid org]]
                          (d/option {:value uuid} (:name org)))
                        (get-in data [:orgs :elements])))
              (= action "user_grant")
              (i/input {:type "select" :value target
                        :on-change (->state owner :target)}
                       (d/option)
                       (map
                        (fn [[uuid user]]
                          (d/option {:value uuid} (:name user)))
                        (get-in data [:users :elements])))
              :else "")
            (if (#{"user_grant" "role_grant"} action)
              (d/span
               " permissions to "
               (i/input {:type "select" :value permission :id "permission-select"
                         :on-change (->state owner :permission)}
                        (map
                         (fn [[val {title :title}]]
                           (d/option {:value val} title))
                         (condp = actor
                           "vm_create"      perms/vm-perms
                           "user_create"    perms/user-perms
                           "dataset_create" perms/dataset-perms
                           [])))
               " the new "
               (condp = actor
                 "vm_create" "VM"
                 "user_create" "User"
                 "dataset_create" "Dataset")
               ""))
            ".")))
         (g/row
          {}
          (g/col
           {}
           (let [payload (mk-trigger state)]
             (b/button
              {:bs-style "primary"
               :onClick #(orgs/add-trigger id actor payload)
               :disabled? (nil? payload)}
              "Create Trigger")
             )))
         (g/row
          {}
          (g/col
           {}
           (table
            {}
            (d/thead
             (d/tr
              (d/th "Event")
              (d/th "Rest")
              (d/th "")))
            (d/tbody
             (map
              (fn [e]
                (d/tr
                 (d/td (trigger-name (:trigger e)))
                 (d/td (role-to-text data e))
                 (d/td
                  (b/button {:bs-size "xsmall"
                             :className "pull-right"
                             :on-click #(orgs/delete-trigger id (:uuid e))}
                            (r/glyphicon {:glyph "remove"})))))
              (sort-by
               #(vector [(:trigger %) (:action %) (:target %)])
               (map (fn [[uuid t]] (assoc t :uuid uuid)) triggers))))
            ))))))))

(defn render-home [{ts :triggers :as data} owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (g/row
        {}
        (g/col
         {:sm 6}
         (p/panel
          {:header (d/h3 "General")
           :list-group
           (lg
            "UUID"     (:uuid data))}))
        (g/col
         {:sm 6}
         (p/panel
          {:header (d/h3 "Triggers")
           :list-group
           (lg
            "Total"            (count (:triggers data))
            "VM Creation"      (count (filter (fn [[_ {t :trigger}]] (= t "vm_create")) ts))
            "User Creation"    (count (filter (fn [[_ {t :trigger}]] (= t "user_create")) ts))
            "Dataset Creation" (count (filter (fn [[_ {t :trigger}]] (= t "dataset_create")) ts)))})))))))

(def sections
  {""          {:key  1 :fn #(om/build render-home %2)      :title "General"}
   "resources" {:key  2 :fn #(om/build render-resources %2)  :title "Resources"}
   "triggers"  {:key  3 :fn #(om/build render-triggers %1 {:opts {:id (:uuid %2)}})  :title "Triggers"}
   "metadata"  {:key  4 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render
  (view/make
   root sections orgs/get
   :mount-fn (fn [uuid data]
               (orgs/list data)
               (users/list data)
               (roles/list data))
   :name-fn :name))
