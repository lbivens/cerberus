(ns cerberus.alert
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [cerberus.utils :refer [goto val-by-id by-id a menu-items]]
   [cerberus.state :refer [delete-state! set-state!]]))


(defn clear [id]
  (delete-state! [:alerts id]))

(defn raise [type text]
  (let [id (str "alert-" (Math/round (* 10000000 (rand))))]
    (js/setTimeout #(set-state! [:alerts id :overlay] false) 1000)
    (js/setTimeout #(clear id) 10000)
    (set-state! [:alerts id] {:id id :overlay true :type type :text text})))
