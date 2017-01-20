(ns cerberus.list.table
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.random :as r]
   [om-bootstrap.pagination :as pg]
   [om-bootstrap.button :as b]
   [cerberus.list.utils :refer [large]]
   [cerberus.config :as config]
   [cerberus.utils :refer [goto make-event menu-items ensure-kw]]
   [cerberus.state :refer [set-state!]]))

(defn cell-opt [opts opt field]
  (if-let [style (opt field)]
    (assoc opts opt style)
    opts))

(def flip-order {"asc" "desc"
                 "desc" "asc"})

(def order-str {"asc" (r/glyphicon {:glyph "chevron-up"})
                "desc" (r/glyphicon {:glyph "chevron-down"})})

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

(defn tbl-header [data owner {:keys [field root]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "tblheadercellc")
    om/IRender
    (render [_]
      (let [id (:id field)
            style {}
            order (get-in data [:sort :order] "asc")
            toggle-fn #(let [flipped (flip-order order)]
                         (config/set! [root :sort :order] flipped)
                         (om/transact! data [:sort :order] (constantly flipped)))
            set-fn #(let [s {:field id :order "asc"}]
                      (config/set! [root :sort] s)
                      (om/transact! data [:sort] (constantly s)))]

        (if (= id (ensure-kw (get-in data [:sort :field])))
          (d/td {:style style} (d/a {:onClick toggle-fn
                         :className order} (:title field) " " (order-str order)))
          (d/td {:style style} (d/a {:onClick set-fn} (:title field))))))))

(defn tbl-headers [data owner {:keys [fields actions root]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "tblheaderc")
    om/IRender
    (render [_]
      (d/thead
       (d/tr
        (map #(om/build tbl-header data {:opts {:field % :root root}}) fields)
        (if actions
          (d/td {:class "actions"})))))))

(defn cell-attrs [e]
  (-> {}
      (cell-opt :style e)
      (cell-opt :class e)))

(defn tbl-cell [set-filter {txt :text render-fn :render-fn quick-filter :quick-filter filter-txt :filter-text :as e }]
  (if quick-filter
    (d/td (cell-attrs e)
          (r/glyphicon {:glyph "pushpin"
                        :class "filterby"
                        :on-click (make-event #(set-filter (str (name (:id e)) ":" filter-txt)))}) " "
                        (if render-fn
                          (render-fn txt)
                          txt))
    (d/td (cell-attrs e)
          (if render-fn
            (render-fn txt)
            txt))))

(defn tbl-row [data owner {:keys [root actions fields set-filter]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "tblrowc")
    om/IRenderState
    (render-state [_ _]
      (let [style {}
            cells (:row data)]
        (d/tr
         {:on-click #(goto (str "/" (name root) "/" (:uuid data)))  :style style}
         (map (partial tbl-cell set-filter) cells)
         (if actions
           (d/td {:class "actions"}
                 (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                              :on-click (make-event identity)}
                             (apply menu-items (actions data))))))))))

(defn render [data elements {:keys [root actions fields set-filter]}]
  (if (not (:sort data))
    (om/update! data :sort {}))
  (d/div
   {:class large :id "list-tbl"}
   (let [opts {:fields fields :root root :actions actions :set-filter set-filter}]
     (table
      {:condensed? true :hover? true}
      (om/build tbl-headers data {:opts opts})
      (d/tbody
       (om/build-all tbl-row elements {:key :uuid :opts opts}))))))
