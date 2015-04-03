(ns jingles.list.utils
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.pagination :as pg]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [jingles.match :as jmatch]
   [jingles.utils :refer [goto val-by-id make-event value-by-key menu-items by-id]]
   [jingles.state :refer [set-state! update-state!]]))


(def large "hidden-xs hidden-ms")

(def small "visible-xs-block visible-ms-block")

(defn show-field [{key :key formater :formater :as field} element]
  (let [txt (value-by-key key element)]
    (if (and formater txt)
      (formater txt)
      txt)))

(defn get-filter-field [field element]
  (if-let [key (:filter-key field)]
    (value-by-key key element)
    (value-by-key (:key field) element)))

(defn expand-fields [config selected]
  (let [fields (:fields config)]
    (map (fn [field] (fields field)) selected)))


(defn filter-field [root text]
  #_(let [cur (conf/get [root :filter] "")
        cur (if (empty? cur) cur (str cur " "))]
      (make-event #(conf/write! [root :filter]  (str  cur text))))
  (make-event identity))
