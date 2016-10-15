(ns cerberus.metadata
  (:require
   [clojure.set]
   [clojure.string :refer [split join]]
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [om-bootstrap.panel :as p]
   [cerberus.alert :refer [alerts]]
   [cerberus.api :as api]
   [cerberus.utils :refer [->state event-val]]
   [cerberus.utils :refer [grid-row row]]
   [cerberus.utils :refer [make-event menu-items]]))

(enable-console-print!)

(defn flatten-map
  ([form separator]
     (into {} (flatten-map form separator nil)))
  ([form separator pre]
     (mapcat (fn [[k v]]
               (let [prefix (if pre (str pre separator (name k)) (name k))]
                 (if (map? v)
                   (flatten-map v separator prefix)
                   [[(keyword prefix) v]])))
               form)))

(defn valid-json [value]
  (try 
    (js/JSON.parse value)
    (= (subs (str value) 0 1) "{")
  (catch js/Object e
    false)))

(defn save [state opts]
  (let
    [orig (flatten-map (:metadata_orig state) "/")
     mod (flatten-map (js->clj (js/JSON.parse (:metadata state))) "/")
     del_keys (clojure.set/difference (set (keys orig)) (set (keys mod)))
     add_keys (clojure.set/difference (set (keys mod)) (set (keys orig)))
     int_keys (clojure.set/intersection (set (keys mod)) (set (keys orig)))
    ]

    (doseq [k1 del_keys]
      (api/delete (:root opts) [(:uuid opts) :metadata (subs (str k1) 1)] {}))

    (doseq [k2 add_keys]
      (let 
        [pathparts (split (subs (str k2) 1) "/")
         lastkey (first (reverse pathparts))
         shortpath (join "/" (reverse (next (reverse pathparts))))
         newvalue {lastkey (k2 mod)}
        ]
        (api/put (:root opts) [(:uuid opts) :metadata shortpath] newvalue {})))

    (doseq [k3 int_keys]
      (if (not= (k3 orig) (k3 mod)) 
        (let 
        [pathparts (split (subs (str k3) 1) "/")
         lastkey (first (reverse pathparts))
         shortpath (join "/" (reverse (next (reverse pathparts))))
         newvalue {lastkey (k3 mod)}
        ]
        (api/put (:root opts) [(:uuid opts) :metadata shortpath] newvalue {}))))))

(defn render [data owner config]
  (reify
    om/IDisplayName
    (display-name [_]
      "metadata-well")
    om/IInitState
    (init-state [_]
       {
        :metadata (if (= (js/JSON.stringify (clj->js data) nil 2) "[]") 
                       "{}" (js/JSON.stringify (clj->js data) nil 2) )
        :metadata_orig data
      })
    om/IRenderState
    (render-state [_ state]
      (g/grid
       {}
       (row
        (g/col
         {:xs 12}
         (b/button
          {:bs-style "primary"
           :class "rightbtn"
           :on-click #(save state config)
           :disabled? (false? (valid-json (:metadata state)))
          }
          "Save")))
       (row  
        (g/col
          {:md 12}
          (p/panel
            (if (valid-json (:metadata state)) 
              {:bs-style nil}
              {:header (d/h3 "Invalid JSON Object")
               :bs-style "danger"})
            (i/input
              {:type "textarea"
               :rows "10"
               :value (:metadata state ) 
               :on-change (->state owner :metadata) 
               }))))))))