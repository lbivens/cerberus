(ns jingles.routing
  (:require
   [jingles.state :refer [set-view!]]
   [goog.events :as events]
   [jingles.vms :as vms]
   [jingles.datasets :as datasets]
   [jingles.servers :as servers]
   [goog.history.EventType :as EventType]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(enable-console-print!)

(secretary/set-config! :prefix "#")

(defroute "/" {:as params}
  (set-view! :home))

(defroute "/vms" {:as params}
  (vms/full-list)
  (set-view! :vm-list))

(defroute "/datasets" {:as params}
  (datasets/full-list)
  (set-view! :dataset-list))

(defroute "/servers" {:as params}
  (servers/full-list)
  (set-view! :server-list))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
