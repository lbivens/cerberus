(ns jingles.list
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [jingles.utils :refer [goto]]
            [jingles.state :refer [set-state!]]))

(defn value-by-key [key element]
  (cond
   (keyword? key) (key element)
   (fn? key) (key element)
   (list? key) (get-in element (vec key))
   :else (do (pr (type key)) "-")))

(defn show-field [field element]
  (value-by-key (:key field) element))

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
        ;; If we're on a page that is too lage
        ;; we jump back to the first page
        page (if (< length (* page size)) 1 page)]
    {:length length
     :page page
     :size size
     :list (do-paginate elements size page)}))

(defn do-sort [config state]
  (let [sort (:sort state)
        list (:list state)
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

(defn sort-and-paginate [config state]
  (if-let [sort (:sort state)]
    (paginate (do-sort config state) state)
    (paginate (:list state) state)))

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
      (d/td (d/a #js{:onClick #(set-state! [root :sort :order] (flip-order order))
                     :className (order-class order)} (:title field) " " (order-str order)))
      (d/td (d/a #js{:onClick #(set-state! [root :sort] {:field id :order :asc})} (:title field))))))

(defn tbl-headers [root sort fields]
  (d/thead
   (d/tr
    (map (partial tbl-header root sort) fields))))

(defn page-link [root current page]
  [" " (d/a #js{:onClick (fn [] (set-state! [root :page] page))
                :className (if (= page current) "current-page" "")}
            page)])

(defn pagination [root data]
  (let [page-count (Math/ceil (/ (:length data) (:size data)))
        current (:page data)]
    (if (> page-count 1)
      (map
       (partial page-link root current)
       (range 1 (inc page-count))))))

(defn tbl [config state]
  (let [root (:root config)
        fields(expand-fields config (:fields state))
        elements (sort-and-paginate config state)]
    (d/div
     nil
     (table
      {:striped? true :bordered? true :condensed? true :hover? true}
      (tbl-headers root (:sort state) fields)
      (d/tbody
       (map
        (fn [e] (d/tr
                 #js{:onClick #(goto (str "/" (name root) "/" (:uuid e)))}
                 (map
                  (fn [field]
                    (d/td (show-field field e)))
                  fields)))
        (map #(get-in state [:elements %]) (:list elements)))))
     (pagination root elements))))


(defn view [config app]
  (let [root (:root config)
        title (:title config)
        state (root app)]
    (d/div
     nil
     (d/h1 nil title)
     (tbl config state))))
