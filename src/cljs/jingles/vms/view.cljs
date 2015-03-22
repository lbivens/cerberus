(ns jingles.vms.view
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [jingles.http :as http]
            [jingles.api :as api]))

(def root :vms)

(def sub-element (partial api/get-sub-element))

(defn get-package [element]
  (sub-element :packages :package [:name] element))

(defn get-dataset [element]
  (sub-element :datasets :dataset [:name] element))

(defn render [app]
  (do
    (pr (keys (get-in app [root])))
    (let [uuid (get-in app [root :selected])
          element (get-in app [root :elements uuid])]
      (d/div
       nil
       (d/h1 nil (get-in element [:config :alias]))
       "uuid: " (:uuid element) (d/br)
       "package: " (get-package element) (d/br)
       "dataset: " (get-dataset element) (d/br)
       (r/well
        {}
        (. js/JSON (stringify (clj->js element))))))))
