(ns jingles.vms.view
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [om-bootstrap.button :as b]
   [jingles.utils :refer [goto grid-row val-by-id]]
   [jingles.http :as http]
   [jingles.api :as api]
   [jingles.vms.api :as vms]
   [jingles.vms.api :refer [root]]
   [jingles.packages.api :as packages]
   [jingles.state :refer [set-state!]]
   [jingles.utils :refer [make-event menu-items]]
   [jingles.fields :refer [fmt-bytes fmt-percent]]))


(def sub-element (partial api/get-sub-element))

(defn get-package [element]
  (sub-element :packages :package [:name] element))

(defn get-dataset [element]
  (sub-element :datasets :dataset [:name] element))

(defn render-home [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [conf (:config data)
            owner (api/get-sub-element :orgs :owner identity data)
            package (api/get-sub-element :packages :package identity data)
            dataset (api/get-sub-element :datasets :dataset identity data)
            services (:services data)]
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
         (count (filter (fn [[_ state]] (= state "disabled")) services)))))))

(defn render-services [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [services (:services data)]
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
            services))))))))

(defn render-logs [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [logs (:log data)]
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
            logs))))))))


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

(defn render-networks [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [networks (get-in data [:config :networks])
            rows (partition 4 4 nil networks)]
        (r/well
         nil
         (g/grid
          nil
          (map #(g/row nil (map render-network %)) rows)))))))

(defn cmp-vals [package cmp-package val]
  (if-let [cmp-vap (cmp-package val)]
    (let [val (if package (package val) 0)
          diff (- cmp-vap val)]
      (cond
        (> diff 0) [val " (+" diff ")"]
        (< diff 0) [val " (" diff ")"]
        :else [val]))
    [(if package (package val) 0)]))

(defn apply-fmt [fmt v & rest]
  (concat [(fmt v)] rest))

(defn render-package [app element]
  (let [current-package (:package element)
        packages (get-in app [:packages :elements])
        package (get-in packages [current-package])
        cmp-pkg (get-in app [:tmp :pkg] {})
        cmp-vals (partial cmp-vals package cmp-pkg)]
    (packages/list app)
    (r/well
     {}
     (grid-row
      (g/col
       {:md 4}
       (p/panel
        {:header (if package (:name package) "custom")
         :list-group
         (d/ul {:class "list-group"}
               (group-li "CPU: "    (apply apply-fmt fmt-percent (cmp-vals :cpu_cap)))
               (group-li "Memory: " (apply apply-fmt (partial fmt-bytes :mb) (cmp-vals :ram)))
               (group-li "Quota: "  (apply apply-fmt (partial fmt-bytes :gb) (cmp-vals :quota))))}))
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
                  (let [cmp #(let [v (if package (package %1) 0)]
                               (cond
                                 (> %2 v) (r/glyphicon {:glyph "chevron-up"})
                                 (< %2 v) (r/glyphicon {:glyph "chevron-down"})
                                 :else ""))
                        td (fn [v f] (d/td (f (pkg v)) (cmp v (pkg v))))]
                    (d/tr
                     {:class (if (= uuid current-package) "current" "")
                      :on-mouse-over (fn [e] (set-state! [:tmp :pkg] pkg))
                      :on-mouse-leave (fn [e] (set-state! [:tmp :pkg] {}))}
                     (d/td name)
                     (td :cpu_cap fmt-percent)
                     (td :ram     #(fmt-bytes :mb %))
                     (td :quota   #(fmt-bytes :gb %)))))
                packages))))))))

(defn snapshot-row  [vm [uuid {comment :comment timestamp :timestamp
                               state :state size :size}]]
  (d/tr
   (d/td (name uuid))
   (d/td comment)
   (d/td (str (js/Date. (/ timestamp 1000))))
   (d/td state)
   (d/td (fmt-bytes :b size))
   (d/td {:class "actions no-carret"}
         (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                      :on-click (make-event identity)}
                     (menu-items
                      ["Roll Back" #(pr "rollback" vm)]
                      ["Delete" #(vms/delete-snapshot vm uuid)])))))

(defn snapshot-table [vm snapshots]
  (g/col
   {:md 11}
   (table
    {:id "snapshot-table"}
    (d/thead
     {}
     (d/td "UUID")
     (d/td "Comment")
     (d/td "Timestamp")
     (d/td "State")
     (d/td "Size")
     (d/td {:class "actions"}))
    (apply d/tbody
           {}
           (map
            (partial snapshot-row vm)
            (sort-by (fn [[_ {t :timestamp}]] t) snapshots))))))

(defn render-snapshots [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (grid-row
        (g/col
         {:md 12}
         (i/input
          {:label "New Snapshot"}
          (grid-row
           (g/col
            {:xs 10}
            (i/input {:type :text
                      :placeholder "Snapshot Comment"
                      :id "snapshot-comment"
                      }))
           (g/col {:xs 2}
                  (b/button {:bs-style "primary"
                             :wrapper-classname "col-xs-2"
                             :on-click (fn [] (pr (:uuid data) (val-by-id "snapshot-comment"))
                                         (if (not (empty? (val-by-id "snapshot-comment")))
                                           (vms/snapshot (:uuid data) (val-by-id "snapshot-comment"))))} "Create")))))
        (snapshot-table (:uuid data) (:snapshots data)))))))

(defn render-backups [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str (:backups data))))))

(defn render-fw-rules [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str (:fw_rules data))))))

(defn render-metadata [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str (:metadata data))))))

(defn nice-metrics [metrics]
  (reduce #(assoc %1 (:n %2) (:v %2)) metrics))

(defn render-metrics [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str (nice-metrics (:metrics data)))))))



(def sections {""          {:key  1 :fn #(om/build render-home %2)      :title "General"}
               "networks"  {:key  2 :fn #(om/build render-networks %2)  :title "Networks"}
               "package"   {:key  3 :fn render-package   :title "Package"}
               "snapshots" {:key  4 :fn #(om/build render-snapshots %2) :title "Snapshot"}
               "backups"   {:key  5 :fn #(om/build render-backups %2)   :title "Backups"}
               "services"  {:key  6 :fn #(om/build render-services %2)  :title "Services"}
               "logs"      {:key  7 :fn #(om/build render-logs %2)      :title "Logs"}
               "fw-rules"  {:key  8 :fn #(om/build render-fw-rules %2)  :title "Firewall"}
               "metrics"   {:key  9 :fn #(om/build render-metrics %2)   :title "Metrics"}
               "metadata"  {:key 10 :fn #(om/build render-metadata %2)  :title "Metadata"}})
;; This is really ugly but something is crazy about the reify for OM here
;; this for will moutnt and will unmoutn are not the same and having timer in
;; let does not work either so lets "MAKE ALL THE THINGS GLOBAL!"

(def timer (atom))

(defn stop-timer! []
  (if @timer
    (js/clearInterval @timer))
  (reset! timer nil))

(defn start-timer! [uuid]
  (stop-timer!)
  (reset! timer (js/setInterval #(vms/metrics uuid) 1000)))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "vmview")
    om/IWillMount
    (will-mount [_]
      (start-timer! (get-in data [root :selected])))
    om/IWillUnmount
    (will-unmount [_]
      (stop-timer!))
    om/IRenderState
    (render-state [_ _]
      (let [uuid (get-in data [root :selected])
            element (get-in data [root :elements uuid])
            section (get-in data [root :section])
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
           (f data element)
           (goto (str "#/vms/" uuid))))))))
