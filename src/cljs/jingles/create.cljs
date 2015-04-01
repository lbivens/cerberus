(ns jingles.create
  (:refer-clojure :exclude [print])
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
   [jingles.api :as api]
   [jingles.config :as conf]
   [jingles.http :as http]
   [jingles.utils :refer [make-event val-by-id str->int]]))

(defn default-validator [data-type]
  (condp = data-type
    :string #(not (empty? %))
    :integer integer?))

(defn mk-validator [{validator :validator data-type :data-type
                     optional :optional
                     :or {data-type :string}}]
  (let [validator (or validator (default-validator data-type))]
    #(or (and optional (empty? %)) (validator %))))


(defn to-dt [data-type val]
  (condp = data-type
    :integer (str->int val)
    val))

(defn from-dt [data-type val]
  (condp = data-type
    val))



(defn validate-data [spec]
  (let [data (conf/get [:add :data])
        results (map
                 (fn [{key :key
                       :or {data-type :string} :as field}]
                   (let [path (if (vector? key) key [key])
                         val (get-in data path)
                         validator (mk-validator field)]
                     (validator val))) spec)]
    (every? identity results)))

(defn validate-data! [spec]
  (let [result (validate-data spec)]
    (if (not= (conf/get [:add :valid]) result)
      (conf/write! [:add :valid] result))))

(defn input [spec {id :id key :key validator :validator label :label type :type data-type :data-type
                   unit :unit options :options optional :optional
                   :or {data-type :string type :input} :as field}]
  (let [data-path (concat [:add :data] (if (vector? key) key [key]))
        view-path (concat [:add :view] (if (vector? key) key [key]))
        validator (mk-validator field)
        val (conf/get view-path "")
        data-val (conf/get data-path)]
    (i/input {:type type :label label
              :label-classname "col-xs-1"
              :wrapper-classname "col-xs-11"
              :id id
              :addon-after unit
              :has-feedback? true
              :bs-style (if (validator data-val) "success" "error")
              :on-change #(let [v (val-by-id id)
                                dv (to-dt data-type v)]
                            (conf/write! view-path v)
                            (if key
                              (conf/write! data-path dv))
                            (validate-data! spec))
              :value (from-dt data-type val)}
             (map (fn [opt]
                    (if (string? opt)
                      (d/option {:value opt} opt)
                      (d/option {:value (second opt)} (first opt))))
                  (if (and options optional)
                    (concat [["None" nil]] options)
                    options)))))


(defn render [app & spec]
  (d/form {:class "form-horizontal"}
          (map
           (fn [{type :type :as data :or {type :input}}]
             (condp = type
               :select   (input spec (assoc data :type "select"))
               :input    (input spec (assoc data :type "text"))
               :password (input spec (assoc data :type "password"))))
           spec)))

(defn print []
  (.log js/console (clj->js (conf/get [:add :data]))))
