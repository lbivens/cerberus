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
   [jingles.list.utils :refer [show-field get-filter-field expand-fields filter-field large small]]
   [jingles.utils :refer [goto val-by-id make-event value-by-key menu-items by-id]]
   [jingles.state :refer [set-state! update-state!]]))


(defn list-panel [data owner {:keys [root actions]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "listpanelc")
    om/IRender
    (render [_]
      (p/panel {:class "list-panel"
                :header [(:name data)
                         (if actions
                           (d/div {:class "pull-right"}
                                  (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                                               :on-click (make-event identity)}
                                              (apply menu-items (actions data)))))]}
               (map
                (fn [field]
                  (d/div
                   #_(r/glyphicon {:glyph "pushpin"
                                   :class "filterby"
                                   :on-click (filter-field root (str (name (:id field)) ":" txt))})
                   (d/span {:class "field-label"} (:title field) ":")
                   (d/span {:class "value"} (:text field))))
                (:row data))))))

(defn well [data elements {:keys [config state root actions fields] parent :owner}]
  (d/div
   {:class small}
   (om/build-all list-panel elements {:key :uuid :opts {:root root :actions actions}})))
