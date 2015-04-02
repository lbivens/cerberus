(ns jingles.list
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.pagination :as pg]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [jingles.match :as jmatch]
   [jingles.utils :refer [goto val-by-id make-event value-by-key menu-items by-id]]
   [jingles.state :refer [set-state! update-state!]]))

(def large "hidden-xs hidden-ms")

(def small "visible-xs-block visible-ms-block")

(defn show-field [{key :key formater :formater :as field} element]
  (let [txt (value-by-key key element)]
    (if (and formater txt)
      (formater txt)
      txt)))

(defn get-filter-field [field element]
  (if-let [key (:filter-key field)]
    (value-by-key key element)
    (value-by-key (:key field) element)))

(defn expand-fields [config selected]
  (let [fields (:fields config)]
    (map (fn [field] (fields field)) selected)))

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

(def flip-order {:asc :desc
                 :desc :asc})

(def order-class {:asc "asc"
                  :desc "desc"})

(def order-str {:asc (r/glyphicon {:glyph "chevron-up"})
                :desc (r/glyphicon {:glyph "chevron-down"})})

(defn page-click-fn [root page]
  (make-event #(set-state! [root :page] page)))

(defn page-link [root current page]
  (pg/page {:on-click (page-click-fn root page)
            :active? (= page current)}
           page))

(defn pagination [root data]
  (let [page-count (Math/ceil (/ (:length data) (:size data)))
        current (:page data)
        last (:last data)]
    (if (> page-count 1)
      (pg/pagination
       {}
       (concat
        [(if (= 1 current)
           (pg/previous {:on-click (page-click-fn root current)  :disabled? true})
           (pg/previous {:on-click (page-click-fn root (dec current))}))]
        (map
         (partial page-link root current)
         (range 1 (inc page-count)))
        [(if (= last current)
           (pg/next {:on-click (page-click-fn root current) :disabled? true})
           (pg/next {:on-click (page-click-fn root (inc current))}))])))))

(defn filter-field [root text]
  #_(let [cur (conf/get [root :filter] "")
        cur (if (empty? cur) cur (str cur " "))]
      (make-event #(conf/write! [root :filter]  (str  cur text))))
  (make-event identity))

(defn cell-opt [opts opt field]
  (if-let [style (opt field)]
    (assoc opts opt style)
    opts))

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



(defn tbl-header [data owner {:keys [field]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "tblheadercellc")
    om/IRender
    (render [_]
      (let [id (:id field)
            order (get-in data [:order] :asc)]
        (if (= id (get-in data [:field]))
          (d/td (d/a {:onClick #(om/transact! data :order (fn [_] (flip-order order)))
                      :className (order-class order)} (:title field) " " (order-str order)))
          (d/td (d/a {:onClick #(om/transact! data (constantly {:field id :order :asc}))} (:title field))))))))

(defn tbl-headers [data owner {:keys [fields actions]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "tblheaderc")
    om/IRender
    (render [_]
      (d/thead
       (d/tr
        (map #(om/build tbl-header data {:opts {:field %}}) fields)
        (if actions
          (d/td {:class "actions"})))))))

(defn cell-attrs [e]
  (-> {}
      (cell-opt :style e)
      (cell-opt :class e)))

(defn tbl-cell [root {txt :text quick-filter :quick-filter :as e}]
  (if quick-filter
    (d/td (cell-attrs e)
          (r/glyphicon {:glyph "pushpin"
                        :class "filterby"
                        :on-click (filter-field root (str (name (:id e)) ":" txt))}) " " txt)
    (d/td (cell-attrs e)
          txt)))

(defn tbl-row [data owner {:keys [root actions fields]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "tblrowc")
    om/IRender
    (render [_]
      (d/tr
       {:on-click #(goto (str "/" (name root) "/" (:uuid data)))}
       (map (partial tbl-cell root) data)
       (if actions
         (d/td {:class "actions"}
               (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                            :on-click (make-event identity)}
                           (apply menu-items (actions data)))))))))
(defn tbl [data elements {:keys [config state root actions fields] parent :owner}]
  (if (not (:sort data))
    (om/update! data :sort {}))
  (d/div
   {:class large :id "list-tbl"}
   (table
    {:bordered? true :condensed? true :hover? true}
    (om/build tbl-headers (:sort data) {:opts {:fields  fields :actions actions}})
    (d/tbody
     (map 
      #(om/build tbl-row (:row %)
                 {:react-key (:uuid %)
                  :opts {:fields fields :root root :actions actions}})
      (get-in elements [:rendered]))
     #_(om/build-all tbl-row (get-in data [:rendered :rendered]) {:react-key :uuid :fn :row
                                                                  :opts {:fields fields :root root :actions actions}})))
   (pagination root data)))

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
         (tbl section elements {:config config :root root :actions actions :fields display-fields :owner owner})
         #_(well elements config data root actions display-fields))))))
