(ns jingles.vms.view
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [jingles.http :as http]
            [jingles.api :as api]))

(def root :vms)

(defn get-package [app]
  (api/get-sub-element app [root :element] :name :package "packages/"))

(defn get-dataset [app]
  (api/get-sub-element app [root :element] :name :dataset "datasets/"))

(defn render [app]
  (let [element  (get-in app [root :element])]
    (d/div
     nil
     (d/h1 nil (get-in element [:config :alias]))
     "uuid: " (:uuid element) (d/br)
     "package: " (get-package app) (d/br)
     "dataset: " (get-dataset app) (d/br)
     (r/well
      {}
      (. js/JSON (stringify (clj->js element)))))))
