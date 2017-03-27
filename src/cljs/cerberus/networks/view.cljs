(ns cerberus.networks.view
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g :refer [col]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [lg goto row display val-by-id]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.networks.api :refer [root] :as networks]
   [cerberus.ipranges.api :as ipranges]
   [cerberus.view :as view]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn render-ranges [app owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [id (get-in app [root :selected])
            element (get-in app [root :elements id])
            ipranges (get-in app [:ipranges :elements])
            current-ipranges (sort (or (:ipranges element) []))]
        (r/well
         {}
         (row
          (col
           {:xs 10 :sm 4}
           (i/input
            {:type "select" :id "iprange"}
            (map (fn [[uuid e]] (d/option {:value uuid} (:name e)))
                 (sort-by #(:name (second %)) ipranges))))
          (col
           {:xs 2 :sm 1}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :onClick #(networks/add-iprange id (val-by-id "iprange"))}
            "Add"))
          (col
           {:xs 12 :sm 6}
           (d/ul
            (map
             (fn [uuid]
               (d/li
                (d/a {href (str "#/ipranges/" uuid)} (get-in ipranges [uuid :name]))
                (b/button {:bs-size "xsmall"
                           :className "pull-right"
                           :onClick #(networks/remove-iprange id uuid)}
                          (r/glyphicon {:glyph "remove"}))))
             current-ipranges)))))))))

(defn render-resolvers [app owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [id (get-in app [root :selected])
            element (get-in app [root :elements id])
            current-resolvers (sort (or (:resolvers element) ["8.8.8.8", "8.8.4.4"]))]
        (r/well
         {}
         (row
          (col
           {:xs 10 :sm 4}
           (i/input
            {:type "text" :id "resolver"}))
          (col
           {:xs 2 :sm 1}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :onClick #(networks/add-resolver id (val-by-id "resolver"))}
            "Add"))
          (col
           {:xs 12 :sm 6}
           (d/ul
            (map
             (fn [resolver]
               (d/li
                resolver
                (b/button {:bs-size "xsmall"
                           :className "pull-right"
                           :onClick #(networks/remove-resolver id resolver)}
                          (r/glyphicon {:glyph "remove"}))))
             current-resolvers)))))))))

(defn render-home [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (p/panel
          {:header (d/h3 "General")
           :list-group
           (lg
            "UUID"     (:uuid data)
            "IPRanges" (count (:ipranges data)))})))))

(def sections
  {""           {:key  1 :fn #(om/build render-home %2)     :title "General"}
   "ipranges"   {:key  2 :fn #(om/build render-ranges %1) :title "IP Ranges"}
   "resolvers"  {:key  3 :fn #(om/build render-resolvers %1) :title "Resolvers"}
   "metadata"   {:key  4 :fn #(om/build metadata/render
                                        (:metadata %2) {:opts {:root "networks" :uuid (:uuid %2)}}) :title "Metadata"}})

(def render
  (view/make
   root sections
   networks/get-page
   :mount-fn (fn [uuid data]
               (ipranges/list data))
   :name-fn :name))
