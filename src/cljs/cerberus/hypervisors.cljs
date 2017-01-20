(ns cerberus.hypervisors
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.hypervisors.api :refer [root] :as hypervisors]
   [om-bootstrap.random :as r]
   [cerberus.del :as del]
   [om-bootstrap.button :as b]
   [cerberus.hypervisors.view :as view]
   [cerberus.fields :refer [mk-config]]
   [cerberus.utils :refer [initial-state make-event str->int]]
   [cerberus.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [(del/menue-item uuid)])

(def config
  (mk-config
   root "Hypervisors" actions
   :name        {:title "Name" :key :alias :order -20}
   :version     {:title "Version" :key :version :order 1}
   :os-version  {:title "OS Version" :key [:sysinfo (keyword "Live Image")] :order 2}
   :host        {:title "Host" :key :host :order 3 :show false}
   :last-seen   {:title "Last seen" :key :last_seen
                 :sort-key (fn [h] (* -1  (:last_seen h)))
                 :type [:ago :s] :order 4}
   :uptime      {:title "Uptime" :key (fn [h] (str->int (get-in h [:sysinfo (keyword "Boot Time")])))
                 :sort-key (fn [h] (* -1 (str->int (get-in h [:sysinfo (keyword "Boot Time")]))))
                 :type [:ago :s] :order 5}
   :used-men     {:title "Used Memory" :key [:resources :provisioned-memory]
                  :type [:bytes :mb] :order 6}
   :reserved-men {:title "Reserved Memory" :key [:resources :reserved-memory]
                  :type [:bytes :mb] :order 7}
   :free-men     {:title "Free Memory" :key [:resources :free-memory]
                  :type [:bytes :mb] :order 8}
   :zpool-free   {:title "ZPool Free" :key [:pools :zones :free]
                  :type [:bytes :mb] :order 9  :show false}
   :zpool-used   {:title "ZPool Used" :key [:pools :zones :used]
                  :type [:bytes :mb] :order 10  :show false}
   :zpool-total  {:title "ZPool Size" :key [:pools :zones :size]
                  :type [:bytes :mb] :order 11  :show false}
   :zpool-health {:title "ZPool Health" :key [:pools :zones :health]
                  :order 12  :show false}))

(set-state! [root :fields] (initial-state config))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "hypervisorlistc")
    om/IWillMount
    (will-mount [this]
      (om/update! data [root :filter] "")
      (om/update! data [root :filted] [])
      (om/update! data [root :sort] {})
      (hypervisors/list data))
    om/IRenderState
    (render-state [_ _]
      (condp = (:view data)
        :list (del/with-delete
                data root :alias hypervisors/delete
                (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
