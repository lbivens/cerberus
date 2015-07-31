(ns cerberus.services
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [cerberus.utils :refer [grid-row]]
   [cerberus.utils :refer [make-event menu-items]]))

(defn tbl-row [[srv state] owner {:keys [action uuid]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "service-tr")
    om/IRenderState
    (render-state [_ _]
      (let [s= #(= state %)
            s!= #(not= state %)
            srv (clojure.string/replace (str srv) #"^:" "")
            sa (fn [a] (action uuid srv a))]
        (d/tr
         (d/td srv)
         (d/td state)
         (d/td
          {:class "actions no-carret"}
          (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                       :on-click (make-event identity)}
                      (menu-items
                       ["Enable"   {:class (if (s!= "disabled")     "disabled")} #(sa :enable)]
                       ["Disable"  {:class (if (s=  "disabled")     "disabled")} #(sa :disable)]
                       ["Restart"  {:class (if (s!= "online")       "disabled")} #(sa :restart)]
                       ["Refresh"  {:class (if (s!= "online")       "disabled")} #(sa :refresh)]
                       ["Clear"    {:class (if (s!= "maintenance") "disabled")} #(sa :clear)]
                       ))))))))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "services-well")
    om/IRenderState
    (render-state [_ _]
      (let [services (:services data)
            uuid (:uuid data)]
        (r/well
         {}
         (table
          {:striped? true :condensed? true :hover? true :responsive? true}
          (d/thead
           {:striped? false}
           (d/tr
            {}
            (d/td {} "Service")
            (d/td {} "State")
            (d/td {:class "actions"} "")))
          (d/tbody
           {}
           (om/build-all tbl-row services {:key first :opts (assoc opts :uuid uuid)}))))))))
