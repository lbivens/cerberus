(ns jingles.list
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [om-bootstrap.pagination :as pg]
            [om-bootstrap.button :as b]
            [om-bootstrap.input :as i]
            [jingles.config :as conf]
            [jingles.utils :refer [goto val-by-id make-event value-by-key]]
            [jingles.state :refer [set-state! update-state!]]))

(defn show-field [field element]
  (value-by-key (:key field) element))

(defn get-filter-field [field element]
  (if-let [key (:filter-key field)]
    (value-by-key key element)
    (value-by-key (:key field) element)))

(defn expand-fields [config selected]
  (let [fields (:fields config)]
    (map (fn [field] (fields field)) selected)))


(defn do-paginate [elements size page]
  (let [page (dec page)] ; we need to decrease page by one so we start with page 1 not 0
    (take size (drop (* page size) elements))))

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
     :list (do-paginate elements size page)}))

(defn do-sort [config list state]
  (let [sort (:sort state)
        fields (:fields config)
        elements (:elements state)
        field (:field sort)]
    (if-let [key (or (:sort-key (fields field)) (:key (fields field)))]
      (let [sorted (sort-by (fn [uuid]
                              (value-by-key key (elements uuid))) list)]
        (if (= (:order sort) :desc)
          (reverse sorted)
          sorted))
      list)))

(defn apply-filter [config state]
  (let [filter-str (conf/get-config [(:root config) :filter])
        fields (filter #(not= (:filter %) false) (vals (:fields config)))
        fields (map #(partial get-filter-field %) fields)]
    (if (empty? filter-str)
      (:list state)
      (let [re  (re-pattern filter-str)
            list (:list state)]
        (filter (fn [uuid]
                  (let [e (get-in state [:elements uuid])
                        test-fields (map #(% e) fields)]
                    (some #(re-find re (if (string? %) % (str %))) test-fields))) list)))))

(defn sort-and-paginate [config state]
  (if-let [sort (:sort state)]
    (paginate (do-sort config (apply-filter config state) state) state)
    (paginate (apply-filter config state) state)))

(def flip-order {:asc :desc
                 :desc :asc})
(def order-class {:asc "asc"
                  :desc "desc"})
(def order-str {:asc "v"
                :desc "^"})
(defn tbl-header [root sort field]
  (let [id (:id field)
        order (:order sort)]
    (if (= id (:field sort))
      (d/td (d/a {:onClick #(set-state! [root :sort :order] (flip-order order))
                  :className (order-class order)} (:title field) " " (order-str order)))
      (d/td (d/a {:onClick #(set-state! [root :sort] {:field id :order :asc})} (:title field))))))

(defn tbl-headers [root sort fields]
  (d/thead
   (d/tr
    (map (partial tbl-header root sort) fields))))

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

(defn used-fields [all-fields]
  (let [used-fields (filter #(get-in all-fields [% :show]) (keys all-fields))]
    (sort-by #(get-in all-fields [% :order]) used-fields)))

(defn filter-field [root text]
  (make-event #(conf/set-config! [root :filter] text)))


(defn cell-opt [opts opt field]
  (if-let [style (opt field)]
    (assoc opts opt style)
    opts))

(defn cell-attrs [field]
  (-> {}
      (cell-opt :style field)
      (cell-opt :class field)))

(defn tbl [config state]
  (let [root (:root config)
        fields (expand-fields config (used-fields (conf/get-config [root :fields])))
        elements (sort-and-paginate config state)]
    (d/div
     nil
     (table
      {:striped? false :bordered? true :condensed? true :hover? true :responsive? true}
      (tbl-headers root (:sort state) fields)
      (d/tbody
       (map
        (fn [e] (d/tr
                 {:on-click #(goto (str "/" (name root) "/" (:uuid e)))}
                 (map
                  (fn [field]
                    (let [txt (show-field field e)]
                      (if (or (= txt "") (= (:filter field) false))
                        (d/td (cell-attrs field)
                              txt)
                        (d/td (cell-attrs field)
                              (r/glyphicon {:glyph "pushpin"
                                            :class "bingo"
                                            :on-click (filter-field root (str txt))}) " " txt))))
                  fields)))
        (map #(get-in state [:elements %]) (:list elements)))))
     (pagination root elements))))


(defn toggle-field [field aset]
  (if (contains? aset field)
    (disj aset field)
    (conj aset field)))

(defn view [config app]
  (let [root (:root config)
        fields (conf/get-config [root :fields] (jingles.utils/initial-state config))
        title (:title config)
        state (root app)
        filter (conf/get-config [root :filter])]
    (d/div
     nil
     (d/h1 nil title)
     (i/input {:type "text"
               :id "filter"
               :value filter
               :on-change #(conf/set-config! [root :filter] (val-by-id "filter"))})
     (b/dropdown {:title (r/glyphicon {:glyph "align-justify"})}
                 (map-indexed
                  (fn [idx field]
                    (let [id (:id field)
                          toggle-fn (make-event #(conf/update-config! [root :fields id :show] not))]
                      (b/menu-item
                       {:key idx :on-click toggle-fn}
                       (i/input {:type "checkbox"
                                 :label (:title field)
                                 :on-click toggle-fn
                                 :checked (get-in fields [id :show])}))))
                  (vals (:fields config))))
     (tbl config state))))
