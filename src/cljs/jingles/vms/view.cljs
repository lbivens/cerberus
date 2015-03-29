(ns jingles.vms.view
  (:require [om-tools.dom :as d :include-macros true]
            [om-bootstrap.table :refer [table]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [om-bootstrap.nav :as n]
            [jingles.utils :refer [goto grid-row]]
            [jingles.http :as http]
            [jingles.api :as api]
            [jingles.vms.api :refer [root]]
            [jingles.state :refer [set-state!]]
            [jingles.fields :refer [fmt-bytes fmt-percent]]))


(def sub-element (partial api/get-sub-element))

(defn get-package [element]
  (sub-element :packages :package [:name] element))

(defn get-dataset [element]
  (sub-element :datasets :dataset [:name] element))

(defn render-home [app element]
  (let [conf (:config element)
        owner (api/get-sub-element :orgs :owner identity element)
        package (api/get-sub-element :packages :package identity element)
        dataset (api/get-sub-element :datasets :dataset identity element)
        services (:services element)]
    (r/well
     {}
     "Alias: "          (:alias conf)(d/br)
     "Type: "           (:type conf)(d/br)
     "Max Swap: "       (->> (:max_swap conf) (fmt-bytes :b))(d/br)
     "State: "          (:state conf)(d/br)
     "Memory: "         (->> (:ram conf) (fmt-bytes :mb))(d/br)
     "Resolvers: "      (clojure.string/join ", " (:resolvers conf))(d/br)
     "DNS Domain: "     (:dns_domain conf)(d/br)
     "Quota: "          (->> (:quota conf) (fmt-bytes :gb))(d/br)
     "I/O Priority: "   (:zfs_io_priority conf)(d/br)
     "CPU Shares: "     (:cpu_shares conf)(d/br)
     "CPU Cap: "        (-> (:cpu_cap conf) fmt-percent)(d/br)
     "Owner: "          (:name owner)(d/br)
     "Autoboot: "       (:autoboot conf)(d/br)
     "Dataset: "        (:name dataset)(d/br)
     "Created: "        (:created_at conf)(d/br)
     "Backups: "        (count (:backups conf))(d/br)
     "Snapshots: "      (count (:backups conf))(d/br)
     "Firewall Rules: " (count (:fw_rules conf))(d/br)
     "Services: "       (count (filter (fn [[_ state]] (= state "maintainance")) services)) "/"
     (count (filter (fn [[_ state]] (= state "online")) services)) "/"
     (count (filter (fn [[_ state]] (= state "disabled")) services)))))

(defn render-services [app element]
  (let [services (:services element)]
    (r/well
     {}
     (table
      {:striped? true :bordered? true :condensed? true :hover? true :responsive? true}
      (d/thead
       {:striped? false}
       (d/tr
        {}
        (d/td {} "Service")
        (d/td {} "State")))
      (d/tbody
       {}
       (map
        (fn [[srv state]]
          (d/tr
           (d/td (clojure.string/replace (str srv) #"^:" ""))
           (d/td state)))
        services))))))

(defn render-logs [app element]
  (let [logs (:log element)]
    (r/well
     {}
     (table
      {:striped? true :bordered? true :condensed? true :hover? true :responsive? true}
      (d/thead
       {:striped? false}
       (d/tr
        {}
        (d/td {} "Date")
        (d/td {} "Entry")))
      (d/tbody
       {}
       (map
        (fn [{date :date log :log}]
          (d/tr
           (d/td (str (js/Date. date)))
           (d/td log)))
        logs))))))


(defn group-li [& args]
  (d/li {:class "list-group-item"} args))

(defn render-network [{interface :interface
                       tag       :nic_tag
                       ip        :ip
                       netmask   :netmask
                       gateway   :gateway
                       mac       :mac}]
  (g/col
   {:md 4}
   (p/panel
    {:header interface
     :list-group
     (d/ul {:class "list-group"}
           (group-li "Tag: "     tag)
           (group-li "IP: "      ip)
           (group-li "Netmask: " netmask)
           (group-li "Gateway: " gateway)
           (group-li "MAC: "     mac))})))

(defn render-networks [app element]
  (let [networks (get-in element [:config :networks])
        rows (partition 4 4 nil networks)]
    (r/well
     nil
     (g/grid
      nil
      (map #(g/row nil (map render-network %)) rows)))))

(defn cmp-vals [package cmp-package val]
  (do
    (if-let [cmp-vap (cmp-package val)]
      (let [val (package val)
            diff (- cmp-vap val)]
        (cond
         (> diff 0) [val " (+" diff ")"]
         (< diff 0) [val " (" diff ")"]
         :else val))
      (package val))))

(defn render-package [app element]
  (let [current-package (:package element)
        packages (get-in app [:packages :elements])
        package (get-in app [:packages :elements current-package])
        cmp-pkg (get-in app [:tmp :pkg] {})
        cmp-vals (partial cmp-vals package cmp-pkg)]
    (jingles.packages.api/list)
    (r/well
     {}
     (grid-row
      (g/col
       {:md 4}
       (p/panel
        {:header (:name package)
         :list-group
         (d/ul {:class "list-group"}
               (group-li "CPU: "    (-> (cmp-vals :cpu_cap) fmt-percent))
               (group-li "Memory: " (->> (cmp-vals :ram) (fmt-bytes :mb)))
               (group-li "Quota: "  (->> (cmp-vals :quota) (fmt-bytes :gb))))}))
      (g/col
       {:md 8}
       (table
        {}
        (d/thead
         {}
         (map d/td
              ["Name" "CPU" "Memory" "Quota"]))

        (apply d/tbody
               {}
               (map
                (fn [[uuid {name :name :as pkg}]]
                  (let [cmp #(cond
                              (> %2 (package %1)) (r/glyphicon {:glyph "chevron-up"})
                              (< %2 (package %1)) (r/glyphicon {:glyph "chevron-down"})
                              :else "")
                        td (fn [v f] (d/td (f (pkg v)) (cmp v (pkg v))))]
                    (d/tr
                     {:class (if (= uuid current-package) "current" "")
                      :on-mouse-over (fn [e] (set-state! [:tmp :pkg] pkg))
                      :on-mouse-leave (fn [e] (set-state! [:tmp :pkg] {}))}
                     (d/td name)
                     (td :cpu_cap fmt-percent)
                     (td :ram     #(fmt-bytes :mb %))
                     (td :quota   #(fmt-bytes :gb %)))))
                packages)))))
     )))

(defn render-snapshots [app element]
  (r/well
   {}
   (pr-str (:snapshots element))))

(defn render-backups [app element]
  (r/well
   {}
   (pr-str (:backups element))))

(defn render-fw-rules [app element]
  (r/well
   {}
   (pr-str (:fw_rules element))))

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
  (let [uuid (get-in app [root :selected])
        element (get-in app [root :elements uuid])
        section (get-in app [root :section])
        key (get-in sections [section :key] 1)]
    (d/div
     {}
     (apply n/nav {:bs-style "tabs" :active-key key}
            (map
             (fn [[section data]]
               (n/nav-item {:key (:key data)
                            :href (str "#/vms/" uuid (if (empty? section) "" (str "/" section)))}
                           (:title data)))
             (sort-by (fn [[section data]] (:key data)) (seq sections))))
     (if-let [f (get-in sections [section :fn] )]
       (f app element)
       (goto (str "#/vms/" uuid))))))
