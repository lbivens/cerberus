(ns jingles.create
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
   [jingles.api :as api]
   [jingles.config :as conf]
   [jingles.utils :refer [make-event val-by-id str->int]]))



(defn default-validator [data-type]
  (condp = data-type
    :string #(not (empty? %))
    :integer #(not= % js/nan)))

(defn validate-data [spec]
  (let [data (conf/get [:add :data])
        results (map
                 (fn [{validator :validator key :key data-type :data-type
                       :or {data-type :string}}]
                   (let [path (if (vector? key) key [key])
                         validator (or validator (default-validator data-type))
                         val (get-in data path)]
                     (validator val))) spec)
        result (every? identity results)]
    (if (not= (conf/get [:add :valid]) result)
      (conf/write! [:add :valid] result))))


(defn to-dt [data-type val]
  (condp = data-type
    :integer (str->int val)
    val))

(defn from-dt [data-type val]
  (condp = data-type
    val))

(defn input [spec {id :id key :key validator :validator label :label type :type data-type :data-type
                   :or {data-type :string}}]
  (let [path (concat [:add :data] (if (vector? key) key [key]))
        validator (or validator (default-validator data-type))
        val (conf/get path "")]
    (i/input {:type type :label label
              :label-classname "col-xs-1"
              :wrapper-classname "col-xs-11"
              :id id
              :has-feedback? true
              :bs-style (if (validator val) "success" "error")
              :on-change #(do
                            (if key
                              (conf/write! path (to-dt data-type (val-by-id id))))
                            (validate-data spec))
              :value (from-dt data-type val)})))

(defn render [app & spec]
  (d/form {:class "form-horizontal"}
          (map
           (fn [{type :type :as data}]
             (condp = type
               :input    (input spec (assoc data :type "text"))
               :password (input spec (assoc data :type "password")))) spec)))
