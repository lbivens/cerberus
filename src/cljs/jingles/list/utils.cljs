(ns jingles.list.utils
  (:require
   [om.core :as om :include-macros true]
   [jingles.match :as jmatch]
   [jingles.utils :refer [make-event value-by-key]]))


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
