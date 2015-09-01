(ns cerberus.view
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.grid :as g]
   [cerberus.utils :refer [goto grid-row display]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn make [root sections get-fn &
            {init-state :init-state
             name-fn :name-fn
             mount-fn :mount-fn
             unmount-fn :unmount-fn}]
  (fn render [data owner opts]
    (reify
      om/IDisplayName
      (display-name [_]
        (str (name root) "detailc"))
      om/IWillMount
      (will-mount [_]
        (let [uuid (get-in data [root :selected])]
          (get-fn uuid)
          (if mount-fn (mount-fn uuid data))))
      om/IWillUnmount
      (will-unmount [_]
        (if unmount-fn (unmount-fn data)))
      om/IInitState
      (init-state [_] init-state)
      om/IRenderState
      (render-state [_ state]
        (let [uuid (get-in data [root :selected])
              element (get-in data [root :elements uuid])
              section (get-in data [root :section])
              key (get-in sections [section :key] 1)
              base (str "/" (name root) "/" uuid)]
          (d/div
           {}
           (if name-fn
             (g/row
              {:class "ctarow"}
              (d/h1
               {:class "cta"}
               (name-fn element))))
           (g/row
            {:class "ctarow"}
            (apply n/nav {:bs-style "tabs" :active-key key}
                   (map
                    (fn [[section data]]
                      (n/nav-item {:key (:key data)
                                   :href (str "#" base (if (empty? section) "" (str "/" section)))}
                                  (:title data)))
                    (sort-by (fn [[section data]] (:key data)) sections))))
           (if-let [f (get-in sections [section :fn])]
             (f data element)
             (goto base))))))))
