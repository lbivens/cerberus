(ns cerberus.ipranges.view
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.view :as view]
   [cerberus.utils :refer [lg]]

   [cerberus.metadata :as metadata]
   [cerberus.ipranges.api :refer [root] :as ipranges]))

(defn render-home [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (g/row
        {}
        (g/col
         {:xs 12 :sm 6}
         (p/panel
          {:header (d/h3 "General")
           :list-group
           (lg
            "UUID"    (:uuid data)
            "Network" (:network data)
            "Gateway" (:gateway data)
            "Netmask" (:netmask data)
            "VLAN"    (:vlan data)
            "TAG"     (:tag data)
            )})
         )
        (g/col
         {:xs 12 :sm 6}
         (p/panel
          {:header (d/h3 "IPs")
           :list-group
           (lg
            "Free" (count (:free data))
            "Used" (count (:used data)))})))))))

(defn render-ips [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (g/row
        {}
        (g/col
         {:xs 12 :sm 6}
         (p/panel
          {:header "Free"}
          (d/ul
           (map d/li (:free data)))))
        (g/col
         {:xs 12 :sm 6}
         (p/panel
          {:header "Used"}
          (d/ul
           (map d/li (:used data))))))))))

(def sections
  {""          {:key  1 :fn #(om/build render-home %2)     :title "General"}
   "ips"       {:key  2 :fn #(om/build render-ips %2)      :title "IPs"}
   "metadata"  {:key  3 :fn #(om/build metadata/render %2) :title "Metadata"}})

(def render (view/make root sections ipranges/get :name-fn :name))
