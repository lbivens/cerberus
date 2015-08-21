(ns cerberus.list.utils
  (:require
   [om.core :as om :include-macros true]
   [cerberus.match :as jmatch]
   [cerberus.utils :refer [make-event value-by-key]]))


(def large "hidden-xs hidden-sm")

(def small "visible-xs-block visible-sm-block")

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

(defn map-with-metadata [map-fn metadata seq]
  (:acc (reduce
          (fn [{:keys [metadata acc]} e]
            (let [[metadata1 e1] (map-fn metadata e)]
              {:metadata metadata1 :acc (conj acc e1)}))
          {:metadata metadata :acc []} seq)))
