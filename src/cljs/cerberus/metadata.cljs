(ns cerberus.metadata
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [cerberus.utils :refer [grid-row]]
   [cerberus.utils :refer [make-event menu-items]]))

(defn display-value [path v]
  (cond
    (map? v) (d/ul
              (map
               (fn [[k v]]
                 (d/li
                  (name k) ": " (display-value (conj path k) v)))
               v))
    (number? v) v
    (string? v) ["\"" (clojure.string/replace v #"\"" "\\\"") "\""]
    (= true v) "true"
    (= false v) "false"
    (empty? v) ""
    :else [(type v) ": " (str v)]))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "metadata-well")
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (display-value [] (:metadata data))))))
