(ns cerberus.create
  (:refer-clojure :exclude [print])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
   [cerberus.api :as api]
   [cerberus.http :as http]
   [cerberus.debug :as dbg]
   [clojure.string :as string]
   [cerberus.utils :refer [make-event val-by-id str->int]]))

(defn default-validator [data-type]
  (condp = data-type
    :string #(not (empty? %2))
    :integer #(integer? %2)))

(defn not-set? [v]
  (or
   (nil? v)
   (and (string? v) (empty? v))))

(defn mk-validator [{validator :validator data-type :data-type
                     optional :optional
                     :or {data-type :string}}]

  (let [validator (or validator (default-validator data-type))]
    #(or (and optional (not-set? %3)) (validator %1 %2 %3))))

(defn strip-ws [v]
  (if (string? v)
    (string/trim v)))

(defn to-dt [data-type val]
  (condp = data-type
    :integer (if (not-set? val)
               nil
               (str->int val))
    (strip-ws val)))

(defn from-dt [data-type val]
  (condp = data-type
    val))

(defn validate-data [data spec]
  (let [results (map
                 (fn [{key :key
                       :or {data-type :string} :as field}]
                   (let [path (if (vector? key) key [key])
                         path (concat [:data] path)
                         view-path (concat [:view] path)
                         val (get-in data path)
                         view (get-in data path)
                         validator (mk-validator field)]
                     (validator data val view))) spec)]

    (every? identity results)))

(defn validate-data! [data spec]
  (let [result (validate-data data spec)]
    (dbg/debug "valid data:"  result data)
    (om/update! data [:valid] result)))

(defn input [data spec {id :id key :key validator :validator label :label type :type data-type :data-type
                        unit :unit options :options optional :optional default :default
                        :or {data-type :string type :input} :as field}]
  (let [data-path (concat [:data] (if (vector? key) key [key]))
        view-path (concat [:view] (if (vector? key) key [key]))
        validator (mk-validator field)
        val (get-in data view-path)
        data-val (get-in data data-path)
        set-fn #(let [v (val-by-id id)
                      dv (to-dt data-type v)
                      data' (assoc-in
                             (if key
                               (assoc-in data data-path dv)
                               data)
                             view-path v)]
                  (om/update! data view-path v)
                  (if key
                    (om/update! data data-path dv))
                  (validate-data! data' spec))]
    (if (and default (nil? val))
      (do
        (om/update! data data-path default)
        (om/update! data view-path (to-dt data-type default))))
    (i/input {:type type :label label
              :label-classname "col-xs-1"
              :wrapper-classname "col-xs-11"
              :id id
              :addon-after unit
              :has-feedback? true
              :bs-style (if (validator data data-val val) "success" "error")
              :on-change set-fn
              :on-blur set-fn
              :value (from-dt data-type val)}
             (map (fn [opt]
                    (if (string? opt)
                      (d/option {:value opt} opt)
                      (d/option {:value (second opt)} (first opt))))
                  (if (and options optional)
                    (concat [["None" nil]] options)
                    options)))))

(defn render [data & spec]
  (d/form
   {:class "form-horizontal"}
   (map
    (fn [{type :type :as field :or {type :input}}]
      (condp = type
        :select   (input data spec (assoc field :type "select"))
        :input    (input data spec (assoc field :type "text"))
        :password (input data spec (assoc field :type "password"))))
    spec)))
