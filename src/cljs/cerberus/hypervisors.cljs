(ns cerberus.hypervisors
  (:refer-clojure :exclude [get list])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [cerberus.list :as jlist]
   [cerberus.hypervisors.api :refer [root] :as hypervisors]
   [om-bootstrap.random :as r]
   [om-bootstrap.modal :as md]
   [om-bootstrap.button :as b]
   [cerberus.hypervisors.view :as view]
   [cerberus.fields :refer [mk-config]]
   [cerberus.utils :refer [initial-state make-event str->int]]
   [cerberus.state :refer [set-state!]]))

(defn actions [{uuid :uuid}]
  [["Delete" #(set-state! [:delete] uuid)]])


#(hypervisors/delete uuid)

(def config
  (mk-config
   root "Hypervisors" actions
   :name {:title "Name" :key :alias :order -20}
   :version {:title "Version" :key :version :order 1}
   :os-version {:title "OS Version" :key [:sysinfo (keyword "Live Image")] :order 2}
   :host {:title "Host" :key :host :order 3 :show false}
   :last-seen {:title "Last seen" :key :last_seen
               :type [:ago :s] :order 4}
   :uptime {:title "Uptime" :key (fn [h] (str->int (get-in h [:sysinfo (keyword "Boot Time")])))
            :type [:ago :s] :order 5}
   :used-men {:title "Used Memory" :key [:resources :provisioned-memory]
              :type [:bytes :mb] :order 6}
   :reserved-men {:title "Reserved Memory" :key [:resources :reserved-memory]
                  :type [:bytes :mb] :order 7}
   :free-men {:title "Free Memory" :key [:resources :free-memory]
              :type [:bytes :mb]  :order 8}))

(set-state! [root :fields] (initial-state config))

(defn delete-modal [data]
  (let [id (:delete data)
        hv (get-in data [root :elements id])]
    (d/div
     {:style {:display (if id "block" "none")} }
     (md/modal
      {:header (d/h4
                "Delete Hypervisor"
                (d/button {:type         "button"
                           :class        "close"
                           :aria-hidden  true
                           :on-click #(set-state! [:delete] nil)}
                          "×"))
       :close-button? false
       :visible? true
       :animate? false
       :style {:display "block"}
       :footer (d/div
                (b/button {:bs-style "danger"
                           :disabled? false
                           :on-click #(do
                                        (hypervisors/delete id)
                                        (set-state! [:delete] nil))}
                          "delete"))}
      "Are you sure that you want to delete the Hypervisor " (d/strong (get-in hv [:alias])) " (" id ")?"))))


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
        :list (d/div
               {}
               (delete-modal data)
               (om/build jlist/view data {:opts {:config config}}))
        :show (om/build view/render data {})))))
