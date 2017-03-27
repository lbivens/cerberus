(ns cerberus.routing
  (:require
   [cerberus.state :refer [set-view! set-state!]]
   [goog.events :as events]

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
   [cerberus.groupings.api :as groupings]

   [cerberus.config :as config]
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
    (vms/metrics uuid)
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
  (set-state! [:datasets :section] "")
  (set-view! :datasets :show))

(defroute "/datasets/:uuid/:section" {:as params}
  (let [uuid (:uuid params)]
    (datasets/get uuid)
    (set-state! [:datasets :selected] uuid)
    (set-state! [:datasets :section] (:section params))
    (set-view! :datasets :show)))

(defroute "/hypervisors" {:as params}
  (set-view! :hypervisors :list))

(defroute "/hypervisors/:uuid" {:as params}
  (hypervisors/get (:uuid params))
  (set-state! [:hypervisors :selected] (:uuid params))
  (set-state! [:hypervisors :section] "")
  (set-view! :hypervisors :show))

(defroute "/hypervisors/:uuid/:section" {:as params}
  (hypervisors/get (:uuid params))
  (set-state! [:hypervisors :selected] (:uuid params))
  (set-state! [:hypervisors :section] (:section params))
  (set-view! :hypervisors :show))

(defroute "/packages" {:as params}
  (set-view! :packages :list))

(defroute "/packages/:uuid" {:as params}
  (packages/get (:uuid params))
  (set-state! [:packages :selected] (:uuid params))
  (set-state! [:packages :section] "")
  (set-view! :packages :show))

(defroute "/packages/:uuid/:section" {:as params}
  (packages/get (:uuid params))
  (set-state! [:packages :selected] (:uuid params))
  (set-state! [:packages :section] (:section params))
  (set-view! :packages :show))

(defroute "/networks" {:as params}
  (set-view! :networks :list))

(defroute "/networks/:uuid" {:as params}
  (networks/get (:uuid params))
  (set-state! [:networks :selected] (:uuid params))
  (set-state! [:networks :section] "")
  (set-view! :networks :show))

(defroute "/networks/:uuid/:section" {:as params}
  (networks/get (:uuid params))
  (set-state! [:networks :selected] (:uuid params))
  (set-state! [:networks :section] (:section params))
  (set-view! :networks :show))

(defroute "/ipranges" {:as params}
  (set-view! :ipranges :list))

(defroute "/ipranges/:uuid" {:as params}
  (ipranges/get (:uuid params))
  (set-state! [:ipranges :selected] (:uuid params))
  (set-state! [:ipranges :section] "")
  (set-view! :ipranges :show))

(defroute "/ipranges/:uuid/:section" {:as params}
  (ipranges/get (:uuid params))
  (set-state! [:ipranges :selected] (:uuid params))
  (set-state! [:ipranges :section] (:section params))
  (set-view! :ipranges :show))

(defroute "/dtrace" {:as params}
  (set-view! :dtrace :list))

(defroute "/dtrace/:uuid" {:as params}
  (dtrace/get (:uuid params))
  (set-state! [:dtrace :selected] (:uuid params))
  (set-state! [:dtrace :section] "")
  (set-view! :dtrace :show))

(defroute "/dtrace/:uuid/:section" {:as params}
  (dtrace/get (:uuid params))
  (set-state! [:dtrace :selected] (:uuid params))
  (set-state! [:dtrace :section] (:section params))
  (set-view! :dtrace :show))

(defroute "/users" {:as params}
  (set-view! :users :list))

(defroute "/users/:uuid" {:as params}
  (users/get (:uuid params))
  (set-state! [:users :selected] (:uuid params))
  (set-state! [:users :section] "")
  (set-view! :users :show))

(defroute "/users/:uuid/:section" {:as params}
  (users/get (:uuid params))
  (set-state! [:users :selected] (:uuid params))
  (set-state! [:users :section] (:section params))
  (set-view! :users :show))

(defroute "/roles" {:as params}
  (set-view! :roles :list))

(defroute "/roles/:uuid" {:as params}
  (roles/get (:uuid params))
  (set-state! [:roles :selected] (:uuid params))
  (set-state! [:roles :section] "")
  (set-view! :roles :show))

(defroute "/roles/:uuid/:section" {:as params}
  (roles/get (:uuid params))
  (set-state! [:roles :selected] (:uuid params))
  (set-state! [:roles :section] (:section params))
  (set-view! :roles :show))

(defroute "/orgs" {:as params}
  (set-view! :orgs :list))

(defroute "/orgs/:uuid" {:as params}
  (orgs/get (:uuid params))
  (set-state! [:orgs :selected] (:uuid params))
  (set-state! [:orgs :section] "")
  (set-view! :orgs :show))

(defroute "/orgs/:uuid/:section" {:as params}
  (orgs/get (:uuid params))
  (set-state! [:orgs :selected] (:uuid params))
  (set-state! [:orgs :section] (:section params))
  (set-view! :orgs :show))

(defroute "/clients" {:as params}
  (set-view! :clients :list))

(defroute "/clients/:uuid" {:as params}
  (clients/get (:uuid params))
  (set-state! [:clients :selected] (:uuid params))
  (set-state! [:clients :section] "")
  (set-view! :clients :show))

(defroute "/clients/:uuid/:section" {:as params}
  (clients/get (:uuid params))
  (set-state! [:clients :selected] (:uuid params))
  (set-state! [:clients :section] (:section params))
  (set-view! :clients :show))

(defroute "/groupings" {:as params}
  (set-view! :groupings :list))

(defroute "/groupings/:uuid" {:as params}
  (groupings/get (:uuid params))
  (set-state! [:groupings :selected] (:uuid params))
  (set-state! [:groupings :section] "")
  (set-view! :groupings :show))

(defroute "/groupings/:uuid/:section" {:as params}
  (groupings/get (:uuid params))
  (set-state! [:groupings :selected] (:uuid params))
  (set-state! [:groupings :section] (:section params))
  (set-view! :groupings :show))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
