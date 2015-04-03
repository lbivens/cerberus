(ns jingles.list
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [jingles.match :as jmatch]
   [jingles.list.table :as table]
   [jingles.list.table :as well]
   [jingles.list.utils :refer [show-field get-filter-field expand-fields filter-field large small]]
   [jingles.utils :refer [val-by-id make-event value-by-key]]))

(defn paginator [size page]
  (comp (take size) (drop (* page size))))

(defn do-paginate [elements size page]
  (let [page (dec page)] ; we need to decrease page by one so we start with page 1 not 0
    (eduction (paginator size page) elements)))

(defn paginate [elements state]
  (let [length (count elements)
        size (or (:page-size state) 20)
        page (or (:page state) 1)
        last (Math/ceil  (/ length size))
        ;; If we're on a page that is too lage
        ;; we jump back to the first page
        page (if (> page last) last page)]
    {:length length
     :page page
     :size size
     :last last
     :list (doall (do-paginate elements size page))}))

(defn do-sort [config list state]
  (let [sort (:sort state)
        fields (:fields config)
        field (keyword (:field sort))]
    (if-let [key (or (:sort-key (fields field)) (:key (fields field)))]
      (let [sorted (sort-by (partial value-by-key key) list)]
        (doall (if (= (keyword (:order sort)) :desc)
                 (reverse sorted)
                 sorted)))
      list)))

(defn apply-filter [config filter-str state]
  (let [list (vals (:elements state))]
    (if (and filter-str (not (empty? filter-str)))
      (let [fields (filter #(not= (:filter %) false) (vals (:fields config)))
            fields (map #(partial get-filter-field %) fields)
            match (jmatch/parse config filter-str)]
        (doall (filter #(jmatch/run match %) list)))
      list)))

(defn sort-and-paginate [config filter state]
  (if-let [sort (:sort state)]
    (paginate (do-sort config (apply-filter config filter state) state) state)
    (paginate (apply-filter config filter state) state)))

(defn toggle-field [field aset]
  (if (contains? aset field)
    (disj aset field)
    (conj aset field)))

(defn search-field [suffix owner fields config]
  (let [field-id (str "filter-" suffix)]
    [(i/input
      {:type "text" :id field-id :value (om/get-state owner :filter)
       :on-change (fn [] (om/set-state! owner :filter (val-by-id field-id)))})
     (b/dropdown
      {:title (r/glyphicon {:glyph "align-justify"})}
      (map-indexed
       (fn [idx field]
         (let [id (:id field)
               toggle-fn (make-event #(om/transact! owner [:fields id :show] not))]
           (b/menu-item
            {:key idx :on-click toggle-fn}
            (i/input
             {:type "checkbox"
              :label (:title field)
              :on-click toggle-fn
              :checked (get-in fields [id :show])}))))
       (vals fields)))]))

(defn used-fields [all-fields]
  (let [used-fields (filter #(get-in all-fields [% :show]) (keys all-fields))]
    (sort-by #(get-in all-fields [% :order]) used-fields)))

(defn pre-render-field [e field]
  (let [txt (show-field field e)]
    {:quick-filter (not (or (= txt "") (= (:filter field) false) (= (:no-quick-filter field) true)))
     :filter-text (get-filter-field field e)
     :text txt
     :id (:id field)
     :class (:class field)
     :style (:style field)}))

(defn pre-render-element [e fields]
  {:uuid (:uuid e)
   :row (map (partial pre-render-field e) fields)})

(defn pre-render [elements fields]
  (assoc elements :rendered  (map #(pre-render-element % fields) (:list elements))))

(defn view [data owner {:keys [config on-mount]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "listc")
    om/IInitState
    (init-state [_]
      {:filter ""
       :order {}})
    om/IRenderState
    (render-state [_ _]
      (let [root (:root config)
            title (:title config)
            actions (:actions config)
            section (get-in data [root])
            fields (get-in data [root :fields] (jingles.utils/initial-state config))
            filter (om/get-state owner :filter)
            display-fields (expand-fields config (used-fields fields))
            elements (pre-render (sort-and-paginate config filter section) display-fields)]
        (d/div
         {:class "listview"}
         (d/h1
          {}
          title
          (d/div
           {:class (str  "filterbar pull-right " large)}
           (search-field "list" owner fields config)))
         (d/div
          {:class (str  "filterbar " small)}
          (search-field "panel" owner fields config))
         (table/render section elements {:config config :root root :actions actions :fields display-fields :owner owner})
         #_(well elements config data root actions display-fields))))))
