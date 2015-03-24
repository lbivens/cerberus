(ns jingles.routing
  (:require
   [jingles.state :refer [set-view! set-state!]]
   [goog.events :as events]
   [jingles.vms :as vms]
   [jingles.datasets :as datasets]
   [jingles.hypervisors :as hypervisors]

   [jingles.packages :as packages]
   [jingles.networks :as networks]
   [jingles.ipranges :as ipranges]
   [jingles.dtrace :as dtrace]
   [jingles.users :as users]
   [jingles.roles :as roles]
   [jingles.orgs :as orgs]

   [jingles.config :as config]
   [goog.history.EventType :as EventType]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(enable-console-print!)

(if-let [token (.get goog.net.cookies "token")]
  (do (set-state! :token token)
      (config/load)))


(secretary/set-config! :prefix "#")

(defroute "/" {:as params}
  (set-view! :home :home))

(defroute "/vms" {:as params}
  (vms/list)
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
  (datasets/list)
  (set-view! :datasets :list))

(defroute "/datasets/:uuid" {:as params}
  (datasets/get (:uuid params))
  (set-state! [:datasets :selected] (:uuid params))
  (set-view! :datasets :show))

(defroute "/hypervisors" {:as params}
  (hypervisors/list)
  (set-view! :hypervisors :list))

(defroute "/hypervisors/:uuid" {:as params}
  (hypervisors/get (:uuid params))
  (set-state! [:hypervisors :selected] (:uuid params))
  (set-view! :hypervisors :show))

(defroute "/packages" {:as params}
  (packages/list)
  (set-view! :packages :list))

(defroute "/packages/:uuid" {:as params}
  (packages/get (:uuid params))
  (set-state! [:packages :selected] (:uuid params))
  (set-view! :packages :show))

(defroute "/networks" {:as params}
  (networks/list)
  (set-view! :networks :list))

(defroute "/networks/:uuid" {:as params}
  (networks/get (:uuid params))
  (set-state! [:networks :selected] (:uuid params))
  (set-view! :networks :show))

(defroute "/ipranges" {:as params}
  (ipranges/list)
  (set-view! :ipranges :list))

(defroute "/ipranges/:uuid" {:as params}
  (ipranges/get (:uuid params))
  (set-state! [:ipranges :selected] (:uuid params))
  (set-view! :ipranges :show))

(defroute "/dtrace" {:as params}
  (dtrace/list)
  (set-view! :dtrace :list))

(defroute "/dtrace/:uuid" {:as params}
  (dtrace/get (:uuid params))
  (set-state! [:dtrace :selected] (:uuid params))
  (set-view! :dtrace :show))

(defroute "/users" {:as params}
  (users/list)
  (set-view! :users :list))

(defroute "/users/:uuid" {:as params}
  (users/get (:uuid params))
  (set-state! [:users :selected] (:uuid params))
  (set-view! :users :show))

(defroute "/roles" {:as params}
  (roles/list)
  (set-view! :roles :list))

(defroute "/roles/:uuid" {:as params}
  (roles/get (:uuid params))
  (set-state! [:roles :selected] (:uuid params))
  (set-view! :roles :show))

(defroute "/orgs" {:as params}
  (orgs/list)
  (set-view! :orgs :list))

(defroute "/orgs/:uuid" {:as params}
  (orgs/get (:uuid params))
  (set-state! [:orgs :selected] (:uuid params))
  (set-view! :orgs :show))


(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
