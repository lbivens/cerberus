(ns jingles.list.table
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.random :as r]
   [om-bootstrap.pagination :as pg]
   [om-bootstrap.button :as b]
   [jingles.list.utils :refer [filter-field large]]
   [jingles.utils :refer [goto make-event menu-items]]
   [jingles.state :refer [set-state!]]))

(defn cell-opt [opts opt field]
  (if-let [style (opt field)]
    (assoc opts opt style)
    opts))

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
(defn render [data elements {:keys [config state root actions fields] parent :owner}]
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
