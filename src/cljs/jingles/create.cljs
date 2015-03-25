(ns jingles.create
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
   [jingles.api :as api]
   [jingles.http :as http]
   [jingles.config :as conf]
   [jingles.utils :refer [initial-state make-event val-by-id]]
   [jingles.state :refer [set-state!]]))



(defn validate-data [spec]
  (let [data (conf/get-config [:add :data])
        results (map
                 (fn [{validator :validator key :key}]
                   (let [path (if (vector? key) key [key])
                         val (get-in data path)]
                     (validator val))) spec)
        result (every? identity results)]
    (if (not= (conf/get-config [:add :valid]) result)
      (conf/set-config! [:add :valid] result))))


(defn input [spec {id :id key :key validator :validator label :label}]
  (let [path (concat [:add :data] (if (vector? key) key [key]))
        val (conf/get-config path "")]
    (i/input {:type "text" :label label
              :label-classname "col-xs-2"
              :wrapper-classname "col-xs-10"
              :id id
              :has-feedback? true
              :bs-style (if (validator val) "success" "error")
              :on-change #(do
                            (conf/set-config! path (val-by-id id))
                            (validate-data spec))
              :value val})))

(defn render [app & spec]
  (d/form {:class "form-horizontal"}
          (map
           (fn [{type :type :as data}]
             (condp = type
               :input (input spec data))) spec)))
