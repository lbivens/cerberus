(ns jingles.routing
  (:require
   [jingles.state :refer [set-view! set-state!]]
   [goog.events :as events]

   [jingles.vms.api :as vms]
   [jingles.datasets.api :as datasets]
   [jingles.hypervisors.api :as hypervisors]

   [jingles.packages.api :as packages]
   [jingles.networks.api :as networks]
   [jingles.ipranges.api :as ipranges]
   [jingles.dtrace.api :as dtrace]
   [jingles.users.api :as users]
   [jingles.roles.api :as roles]
   [jingles.orgs.api :as orgs]

   [jingles.config :as config]
   [goog.history.EventType :as EventType]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(enable-console-print!)

(secretary/set-config! :prefix "#")

(defroute "/" {:as params}
  (set-view! :home :home))

(defroute "/vms" {:as params}
  (set-view! :vms :list))

(defroute "/vms/:uuid" {:as params}
  (let [uuid (:uuid params)]
    (vms/get uuid)
    (set-state! [:vms :selected] uuid)
    (set-state! [:vms :section] "")
    (set-view! :vms :show)))

(defroute "/vms/:uuid/:section" {:as params}
  (let [uuid (:uuid params)]
    (vms/get uuid)
    (set-state! [:vms :selected] uuid)
    (set-state! [:vms :section] (:section params))
    (set-view! :vms :show)))

(defroute "/datasets" {:as params}
  (set-view! :datasets :list))

(defroute "/datasets/:uuid" {:as params}
  (datasets/get (:uuid params))
  (set-state! [:datasets :selected] (:uuid params))
  (set-view! :datasets :show))

(defroute "/hypervisors" {:as params}
  (set-view! :hypervisors :list))

(defroute "/hypervisors/:uuid" {:as params}
  (hypervisors/get (:uuid params))
  (set-state! [:hypervisors :selected] (:uuid params))
  (set-view! :hypervisors :show))

(defroute "/packages" {:as params}
  (set-view! :packages :list))

(defroute "/packages/:uuid" {:as params}
  (packages/get (:uuid params))
  (set-state! [:packages :selected] (:uuid params))
  (set-view! :packages :show))

(defroute "/networks" {:as params}
  (set-view! :networks :list))

(defroute "/networks/:uuid" {:as params}
  (networks/get (:uuid params))
  (set-state! [:networks :selected] (:uuid params))
  (set-view! :networks :show))

(defroute "/ipranges" {:as params}
  (set-view! :ipranges :list))

(defroute "/ipranges/:uuid" {:as params}
  (ipranges/get (:uuid params))
  (set-state! [:ipranges :selected] (:uuid params))
  (set-view! :ipranges :show))

(defroute "/dtrace" {:as params}
  (set-view! :dtrace :list))

(defroute "/dtrace/:uuid" {:as params}
  (dtrace/get (:uuid params))
  (set-state! [:dtrace :selected] (:uuid params))
  (set-view! :dtrace :show))

(defroute "/users" {:as params}
  (set-view! :users :list))

(defroute "/users/:uuid" {:as params}
  (users/get (:uuid params))
  (set-state! [:users :selected] (:uuid params))
  (set-view! :users :show))

(defroute "/roles" {:as params}
  (set-view! :roles :list))

(defroute "/roles/:uuid" {:as params}
  (roles/get (:uuid params))
  (set-state! [:roles :selected] (:uuid params))
  (set-view! :roles :show))

(defroute "/orgs" {:as params}
  (set-view! :orgs :list))

(defroute "/orgs/:uuid" {:as params}
  (orgs/get (:uuid params))
  (set-state! [:orgs :selected] (:uuid params))
  (set-view! :orgs :show))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
