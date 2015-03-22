(ns jingles.routing
  (:require
   [jingles.state :refer [set-view!]]
   [goog.events :as events]
   [jingles.vms :as vms]
   [jingles.datasets :as datasets]
   [jingles.hypervisors :as hypervisors]
   [goog.history.EventType :as EventType]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(enable-console-print!)

(secretary/set-config! :prefix "#")

(defroute "/" {:as params}
  (set-view! :home :home))

(defroute "/vms" {:as params}
  (vms/list)
  (set-view! :vms :list))

(defroute "/vms/:uuid" {:as params}
  (vms/get (:uuid params))
  (set-view! :vms :show))

(defroute "/datasets" {:as params}
  (datasets/list)
  (set-view! :datasets :list))

(defroute "/datasets/:uuid" {:as params}
  (datasets/get (:uuid params))
  (set-view! :datasets :show))

(defroute "/hypervisors" {:as params}
  (hypervisors/list)
  (set-view! :hypervisors :list))

(defroute "/hypervisors/:uuid" {:as params}
  (hypervisors/get (:uuid params))
  (set-view! :hypervisors :show))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
