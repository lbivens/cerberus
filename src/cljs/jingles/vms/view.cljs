(ns jingles.vms.view
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [om-bootstrap.nav :as n]
            [jingles.utils :refer [goto]]
            [jingles.http :as http]
            [jingles.api :as api]))

(def root :vms)

(def sub-element (partial api/get-sub-element))

(defn get-package [element]
  (sub-element :packages :package [:name] element))

(defn get-dataset [element]
  (sub-element :datasets :dataset [:name] element))



(defn render-home [app element]
  (r/well
   {}
   (pr-str (dissoc element :config :services :backups :snapshots :network_mappings :info :log :fw_rules :metadata))
   (d/hr)
   (pr-str (:config element))))

(defn render-networks [app element]
  (r/well
   {}
   (. js/JSON (stringify (clj->js (get-in element [:config :networks]))))))

(defn render-package [app element]
  (r/well
   {}
   (pr-str (:package element))))

(defn render-snapshots [app element]
  (r/well
   {}
   (pr-str (:snapshots element))))

(defn render-backups [app element]
  (r/well
   {}
   (pr-str (:backups element))))

(defn render-services [app element]
  (r/well
   {}
   (pr-str (:services element))))

(defn render-logs [app element]
  (r/well
   {}
   (pr-str (:log element))))

(defn render-fw-rules [app element]
  (r/well
   {}
   (pr-str (:log element))))

(defn render-metadata [app element]
  (r/well
   {}
   (pr-str (:metadata element))))


(def sections {""          {:key 1 :fn render-home      :title "General"}
               "networks"  {:key 2 :fn render-networks  :title "Networks"}
               "package"   {:key 3 :fn render-package   :title "Package"}
               "snapshots" {:key 4 :fn render-snapshots :title "Snapshot"}
               "backups"   {:key 5 :fn render-backups   :title "Backups"}
               "services"  {:key 6 :fn render-services  :title "Services"}
               "logs"      {:key 7 :fn render-logs      :title "Logs"}
               "fw-rules"  {:key 8 :fn render-fw-rules  :title "Firewall"}
               "metadata"  {:key 9 :fn render-metadata  :title "Metadata"}})

(defn render [app]
  (do
    (let [uuid (get-in app [root :selected])
          element (get-in app [root :elements uuid])
          section (get-in app [root :section])
          key (get-in sections [section :key] 1)]
      (pr "key" key)
      (d/div
       {}
       (apply n/nav {:bs-style "tabs" :active-key key}
              (map
               (fn [[section data]]
                 (n/nav-item {:key (:key data)
                              :href (str "#/vms/" uuid (if (empty? section) "" (str "/" section)))}
                             (:title data)))
               (sort-by (fn [[section data]] (pr (:key data)) (:key data)) (seq sections))))
       (if-let [f (get-in sections [section :fn] )]
         (f app element)
         (goto (str "#/vms/" uuid)))))))
