(ns cerberus.groupings.view
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
   [clojure.string :as cstr]
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [om-bootstrap.button :as b]
   [cerberus.utils :refer [lg goto row display ->state]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.groupings.api :as groupings :refer [root]]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state! app-state]]
   [cerberus.view :as view]
   [cerberus.metrics :as metrics]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn print-conf-rows [uuid pfx confs]
  (map
   (fn [[c v]]
     (if (map? v)
       (print-conf-rows uuid (str pfx (name c) ".") v)
       (let [k (str pfx (name c))
             path (cstr/split k #"\.")]
         (pr path)
         (d/tr
          (d/td k)
          (d/td v)
          (d/td
           (b/button {:bs-size "xsmall"
                      :className "pull-right"
                      :on-click #(groupings/delete-config uuid path)}
                     (r/glyphicon {:glyph "remove"})))))))
   confs))

(defn render-config [element owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "hypervisor-characteristics")
    om/IInitState
    (init-state [_]
      {})
    om/IRenderState
    (render-state [_ state]
      (let [confs    (:config element)
            uuid     (:uuid element)
            invalid? (or (empty? (:conf state))  (empty? (:val state)))]
        (r/well
         {}
         (row
          (g/col
           {:md 3}
           (i/input
            {:type "text"
             :placeholder "Configuration"
             :value (:conf state)
             :on-change (->state owner :conf)}))
          (g/col
           {:md 7}
           (i/input
            {:type "text"
             :placeholder "Value"
             :value (:val state)
             :on-change (->state owner :val)}))
          (g/col
           {:md 2}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :on-click #(groupings/set-config uuid (cstr/split (cstr/trim (:conf state)) #"\.") (cstr/trim (:val state)))
             :disabled? invalid?}
            "Set Configuration")))
         (row
          (g/col
           {}
           (table
            {}
            (d/thead
             (d/tr
              (d/th "Configuration")
              (d/th "Value")
              (d/th "")))
            (d/tbody
             (print-conf-rows uuid "" confs))))))))))

(defn render-home [element owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "grouping-home")
    om/IInitState
    (init-state [_]
      {:alias (:alias element)})

    om/IRenderState
    (render-state [_ state]
      (r/well
       {}
       (row
        (g/col
         {:md 6}
         (p/panel
          {:header (d/h3 "General")
           :list-group
           (lg
            "Name"                   (:name element)
            "UUDI"                   (:uuid element)
            "Type"                   (:type element))})))))))


(defn render-elements [app owner {:keys [type element id root]}]
  (reify
    om/IInitState
    (init-state [_]
      {:grouping (or (first (first (dissoc (get-in app [:groupings :elements]) id))) "")})
    om/IRenderState
    (render-state [_ state]
      (if (= "stack" type)
        (let [groupings (dissoc (get-in app [:groupings :elements]) id)
              current-groupings (sort (or (:elements element) []))
              invalid-grouping (set (cons "" current-groupings))]
          (r/well
           {}
           (row
            (g/col
             {:xs 10 :sm 4}
             (i/input
              {:type "select"
               :value (:grouping state)
               :on-change (->state owner :grouping)}
              (map (fn [[uuid e]] (d/option {:value uuid} (:name e)))
                   (sort-by #(:name (second %)) groupings))))
            (g/col
             {:xs 2 :sm 1}
             (b/button
              {:bs-style "primary"
               :className "pull-right"
               :onClick #(groupings/add-element id (:grouping state))
               :disabled? (invalid-grouping (:grouping state))}
              "Add"))
            (g/col
             {:xs 12 :sm 6}
             (d/ul
              (map
               (fn [uuid]
                 (d/li
                  (d/a {href (str "#/groupings/" uuid)} (get-in groupings [uuid :name]))
                  (b/button {:bs-size "xsmall"
                             :className "pull-right"
                             :onClick #(groupings/remove-element id uuid)}
                            (r/glyphicon {:glyph "trash"}))))
               current-groupings))))))
        (let [])))))

(def sections
  {""          {:key  1 :fn  #(om/build render-home %2)     :title "General"}
   "elements"  {:key  2 :fn  #(om/build render-elements %1
                                        {:opts {:id (:uuid %2)
                                                :type (:type %2)
                                                :element %2
                                                :root root}})     :title "Elements"}
   "config"    {:key  3 :fn #(om/build render-config %2)    :title "Configuration"}
   "metadata"  {:key  4 :fn #(om/build metadata/render
    (:metadata %2) {:opts {:root "groupings" :uuid (:uuid %2)}})  :title "Metadata"}})


(def render
  (view/make
   root sections
   :mount-fn (fn [uuid data]
               (groupings/list data))
   groupings/get))
