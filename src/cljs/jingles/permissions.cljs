(ns jingles.permissions
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [jingles.utils :refer [grid-row]]
   [jingles.utils :refer [make-event menu-items]]))


(defn highlight [part]
  (condp = part
    "_" (d/b "_")
    "..." (d/b "...")
    part))

(defn perm [p]
  (pr "PERMS " (map highlight p) #_(interleave (map highlight p) (repeat "->")))
  (apply d/li (butlast  (interleave (map highlight p) (repeat "->")))))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "rules-well")
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (d/ul
        (map perm (:permissions data)))))))
