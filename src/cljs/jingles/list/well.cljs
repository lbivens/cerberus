(ns jingles.list.well
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.pagination :as pg]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [jingles.match :as jmatch]
   [jingles.list.table :as table]
   [jingles.list.utils :refer [show-field get-filter-field expand-fields filter-field large small]]
   [jingles.utils :refer [goto val-by-id make-event value-by-key menu-items by-id]]
   [jingles.state :refer [set-state! update-state!]]))


#_(defn list-panel [root name-field actions fields e]
  (p/panel {:class "list-panel"
            :header [(show-field name-field e)
                     (if actions
                       (d/div {:class "pull-right"}
                              (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                                           :on-click (make-event identity)}
                                          (apply menu-items (actions e)))))]}
           (map
            (fn [field]
              (let [txt (show-field field e)]
                (d/div
                 (r/glyphicon {:glyph "pushpin"
                               :class "filterby"
                               :on-click (filter-field root (str (name (:id field)) ":" txt))})
                 (d/span {:class "field-label"} (:title field) ":")
                 (d/span {:class "value"} txt))))
            fields)))




#_(defn well [elements config state root actions fields]
  (d/div
   {:class small}
   (let [name-field (get-in config [:fields :name])]
     (map
      (partial list-panel root name-field actions fields)
      (:list elements)))))
