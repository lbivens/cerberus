(ns cerberus.orgs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [goto grid-row display]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.orgs.api :refer [root] :as orgs]
   [cerberus.view :as view]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))


(defn render-resources [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (map
        (fn [[uuid resource]]
          (g/col
           {:xs 12 :sm 6}
           (p/panel
            {:header (d/b uuid)}
            (d/dl
             {}
             (map
              (fn [e] [(d/dt (:action e))
                       (d/dd (str (js/Date. (:time e))))])
              (sort-by :time resource))))))
        (sort-by first data))))))

(defn render-triggers [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str data)))))

(defn render-home [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/h3
        (:name data))
       (:uuid data)))))

(def sections
  {""          {:key  1 :fn #(om/build render-home %2)      :title "General"}
   "resources" {:key  2 :fn #(om/build render-resources (:resources %2))  :title "Resources"}
   "triggers"  {:key  3 :fn #(om/build render-triggers (:triggers %2))  :title "Triggers"}
   "metadata"  {:key  4 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections orgs/get :name-fn :name))
