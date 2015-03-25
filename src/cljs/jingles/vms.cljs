(ns jingles.vms
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [jingles.vms.api :refer [root]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.list :as jlist]
   [jingles.vms.view :as view]
   [jingles.utils :refer [initial-state make-event]]
   [jingles.state :refer [set-state!]]))

(def config {:fields {:name {:id :name :title "Name" :key '(:config :alias) :order 0}
                      :cpu {:id :cpu :title "CPU" :key '(:config :cpu_cap) :order 0}
                      :uuid {:id :uuid :title "UUID" :key :uuid :order 1 :class "uuid"}
                      :state {:id :state :title "State" :key :state}
                      :dataset {:id :dataset :title "Dataset"
                                :key (partial api/get-sub-element :datasets :dataset
                                              #(str (:name %) "-" (:version %)))}
                      :package {:id :package :title "Package"
                                :key (partial api/get-sub-element :packages :package [:name])}
                      :actions {:id :actions :title "" :order 99999 :filter false
                                :style {:width "20px"}
                                :key (fn [e]
                                       (let [locked (get-in e [:metadata :jingles :locked] false)
                                             set-lock (partial api/update-metadata root (:uuid e) [:jingles :locked])]
                                         [(b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                                                       :on-click (make-event identity)}
                                                      (if locked
                                                        (b/menu-item
                                                         {:key 1
                                                          :on-click (make-event #(set-lock false))}
                                                         "Unlock")
                                                        (b/menu-item
                                                         {:key 1
                                                          :on-select (make-event #(set-lock true))}
                                                         "Lock"))
                                                      (b/menu-item {:divider? true})
                                                      (if (= (:state e) "running")
                                                        [(b/menu-item {:key 2 :class (if locked "disabled" "")} "Stop")
                                                         (b/menu-item {:key 3 :class (if locked "disabled" "")} "Restart")]
                                                        [(b/menu-item {:key 2 :class (if locked "disabled" "")} "Start")]))]))}}
             :root root
             :title "Machines"})

(set-state! [root :fields] (initial-state config))

(defn list-view [app]
  (jlist/view config app))

(defn show-view [app]
  (. js/JSON (stringify (clj->js (get-in app [root :element])))))

(defn render [app]
  (condp = (:view app)
    :list (list-view app)
    :show (view/render app)))
