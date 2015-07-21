(ns jingles.list
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [jingles.match :as jmatch]
   [jingles.state :refer [set-state! update-state!]]
   [jingles.list.table :as table]
   [jingles.list.well :as well]
   [jingles.list.utils :refer [show-field get-filter-field expand-fields large small]]
   [jingles.utils :refer [val-by-id make-event value-by-key]]))

(defn toggle-field [field aset]
  (if (contains? aset field)
    (disj aset field)
    (conj aset field)))

(defn search-field [suffix owner config]
  (let [field-id (str "filter-" suffix)]
    (i/input
     {:type "text" :id field-id :value (om/get-state owner :filter)
      :on-change (fn [] (om/set-state! owner :filter (val-by-id field-id)))})))

(defn col-selector [data fields]
  (b/dropdown
   {:title (r/glyphicon {:glyph "align-justify"})}
   (map-indexed
    (fn [idx field]
      (let [id (:id field)
            toggle-fn (make-event #(om/transact! data [:fields id :show] not))]
        (b/menu-item
         {:key idx :on-click toggle-fn}
         (i/input
          {:type "checkbox"
           :label (:title field)
           :on-click toggle-fn
           :checked (get-in data [:fields id :show])}))))
    fields)))

(defn used-fields [expanded-fields fields]
  (sort :order (filter #(get-in fields [(:id %) :show]) expanded-fields)))

(defn pre-render-field [e field]
  (let [txt (show-field field e)]
    {:quick-filter (not (or (= txt "") (= (:filter field) false) (= (:no-quick-filter field) true)))
     :filter (not= (:filter field) false)
     :filter-text (get-filter-field field e)
     :title (:title field)
     :text txt
     :id (:id field)
     :class (:class field)
     :style (:style field)}))

(defn pre-render-element [fields name-key e]
  {:uuid (:uuid e)
   :name (value-by-key name-key e)
   :raw e
   :row (mapv (partial pre-render-field e) fields)})

(defn pre-renderer [fields name-key]
  (map (partial pre-render-element fields name-key)))

(defn make-filter [filter-str]
  (if (and filter-str (not (empty? filter-str)))
    (let [f (jmatch/parse filter-str)]
      (map #(assoc % :show (jmatch/run f (:row %)))))
    (map #(assoc % :show true))))

(defn do-sort [list fields sort]
  (let [field (:field sort)]
    (if-let [key (or (:sort-key (fields field)) (:key (fields field)))]
      (let [sorted (sort-by (partial value-by-key key) list)]
        (doall (if (= (keyword (:order sort)) :desc)
                 (reverse sorted)
                 sorted)))
      list)))

(defn pre-render [elements fields filter-str config sort]
  (let [name-key (get-in config [:fields :name :key])]
    (eduction
     (comp (pre-renderer fields name-key)
           (make-filter filter-str))
     (if (:field sort)
       (do-sort elements (:fields config) sort)
       elements))))

(defn mk-filter-field [owner state]
  (fn [text]
    (let [current (:filter state)]
      (if (empty? current)
        (om/set-state! owner :filter text)
        (om/set-state! owner :filter (str current " " text))))))

(defn view [data owner {:keys [config on-mount]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "listc")
    om/IInitState
    (init-state [_]
      {:filter ""
       :fields (jingles.utils/initial-state config)
       :order :asc})
    om/IRenderState
    (render-state [_ state]
      (let [root (:root config)
            title (:title config)
            actions (:actions config)
            section (get-in data [root])
            fields (get-in data [root :fields])
            filter (get-in data [root :filter])
            expanded-fields (expand-fields config (keys fields))
            display-fields (used-fields expanded-fields fields)
            all-rows (pre-render
                      (vals (:elements section))
                      display-fields filter config (:sort section))
            set-filter (mk-filter-field section state)]
        (d/div
         {:class "listview"}
         (d/h1
          {}
          title
          (d/div
           {:class (str  "filterbar pull-right " large)}
           (search-field "list" owner config)
           (col-selector section expanded-fields)))
         (d/div
          {:class (str  "filterbar " small)}
          (search-field "well" owner config)
          (col-selector section expanded-fields))
         (table/render section all-rows {:root root :actions actions :fields display-fields :set-filter set-filter :show fields})
         (well/well section all-rows {:root root :actions actions :set-filter set-filter :show fields}))))))
