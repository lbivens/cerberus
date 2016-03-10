(ns cerberus.list
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [cemerick.url :as url]

   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [cerberus.match :as jmatch]
   [cerberus.config :as config]
   [cerberus.state :refer [set-state! update-state!]]
   [cerberus.debug :as dbg]
   [cerberus.list.table :as table]
   [cerberus.list.well :as well]
   [cerberus.list.utils :refer [show-field get-filter-field expand-fields large small]]
   [cerberus.utils :refer [goto, val-by-id make-event value-by-key str->int]]))

(defn toggle-field [field aset]
  (if (contains? aset field)
    (disj aset field)
    (conj aset field)))

(def search-help-link "https://docs.project-fifo.net/docs/cerberus-search-syntax")
(defn search-field [suffix data]
  (let [field-id (str "filter-" suffix)]
    (i/input
     {:type "text" :id field-id :value (:filter data)
      :on-change (fn [] (om/update! data :filter (val-by-id field-id)))})
    ))

(defn col-selector [data fields root]
  (b/dropdown
   {:title (r/glyphicon {:glyph "align-justify"})}
   (map-indexed
    (fn [idx field]
      (let [id (:id field)
            toggle-fn (make-event #(do
                                     (config/set! [root :fields id :show]
                                                  (not (get-in data [:fields id :show])))
                                     (om/transact! data [:fields id :show] not)))]
        (b/menu-item
         {:key idx :on-click toggle-fn}
         (i/input
          {:type "checkbox"
           :label (:title field)
           :on-click toggle-fn
           :checked (get-in data [:fields id :show])}))))
    (sort-by #(get-in data [:fields (:id %) :order])  fields))))

(defn used-fields [expanded-fields fields]
  (sort-by #(get-in fields [(:id %) :order]) (filter #(get-in fields [(:id %) :show]) expanded-fields)))

(defn pre-render-field [e field]
  (let [txt (show-field field e)]
    {:quick-filter (not (or (= txt "") (= (:filter field) false) (= (:no-quick-filter field) true)))
     :filter (not= (:filter field) false)
     :filter-text (get-filter-field field e)
     :title (:title field)
     :render-fn (:render-fn field)
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
      (filter #(jmatch/run f (:row %))))
    identity))

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

(defn mk-filter-field [data]
  (fn [text]
    (om/update! data :filter text)))

(defn view [data owner {:keys [config on-mount]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "listc")
    om/IInitState
    (init-state [_]
      {:filter ""
       :fields (cerberus.utils/initial-state config)
       :order :asc
       :page 0})
    om/IRenderState
    (render-state [_ state]
      (let [root (:root config)
            title (:title config)
            actions (:actions config)
            section (get-in data [root])
            fields (:fields section)
            filter (:filter section)
            expanded-fields (expand-fields config (keys fields))
            display-fields (used-fields expanded-fields fields)
            all-rows (pre-render
                      (vals (:elements section))
                      display-fields filter config (:sort section))
            href (-> js/window .-location .-href)
            match (re-matches #".*[?]page=([0-9]+)$" href)
            entries (count all-rows)
            sel-page (if match (str->int (second match)) 0)
            page-size 20
            max-page (Math/floor (/ entries page-size))
            page (min max-page sel-page)
            all-rows (drop (* page page-size) all-rows)
            all-rows (take page-size all-rows)
            set-filter (mk-filter-field section)]
        (if (not= page (:page state))
          (om/set-state! owner :page page))
        (if (not= sel-page page)
          (goto (str "/vms?page=" page)))
        (d/div
         {:class "listview"}
         (d/h1
          {}
          title
          (d/div
           {:class (str  "filterbar pull-right " large)}
           (d/a {:href search-help-link :target "_blank" :class "pull-right"
                 :style {:font-size 24}}
                (r/glyphicon {:glyph "question-sign"}))
           (search-field "list" section)
           (col-selector section expanded-fields root)))
         (d/div
          {:class (str  "filterbar " small)}
          (search-field "well" section)
          (col-selector section expanded-fields root))
         (d/div
          {}
          (b/button-group
           {:class "fctabuttons"}
           (b/button {
                      :bs-size "small"
                      :disabled? (= page 0)
                      :on-click #(let [new-page (max 0 (dec (:page state)))]
                                   (om/set-state! owner :page new-page)
                                   (goto (str "/vms?page=" new-page)))} "<")
           (let [pagination-buttons 20
                 p-start (max 0 (- page (/ pagination-buttons 2)))
                 pages (min pagination-buttons (inc (- max-page p-start)))]
             (map (fn [p]
                    (b/button {:bs-size "small"
                               ;;:bs-style (if (= page p) "primary" "danger")
                               :style {:color (if (= page p) "blue" "black")}
                               :on-click #(do (om/set-state! owner :page p)
                                              (goto (str "/vms?page=" p)))} p))
                  (take pages (iterate inc p-start))))
           (b/button {:bs-size "small"
                      :disabled? (= page (dec max-page))
                      :on-click #(let [new-page (min max-page (inc (:page state)))]
                                   (om/set-state! owner :page new-page)
                                   (goto (str "/vms?page=" new-page)))} ">")))
         (table/render section all-rows {:root root :actions actions :fields display-fields :set-filter set-filter :show fields})
         ;(well/well section all-rows {:root root :actions actions :set-filter set-filter :show fields})
         )))))
