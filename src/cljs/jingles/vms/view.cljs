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
   [jingles.services :as services]
   [jingles.metadata :as metadata]
   [jingles.vms.api :refer [root] :as vms]
   [jingles.networks.api :as networks]
   [jingles.view :as view]
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
         "I/O Priority: "   (:zfs_io_pryesiority conf)(d/br)
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

(defn render-network
  [uuid
   {interface :interface
    tag       :nic_tag
    ip        :ip
    netmask   :netmask
    gateway   :gateway
    mac       :mac
    primary   :primary}]
  (g/col
   {:md 4}
   (p/panel
    {:header
     [interface
      (if (not primary)
        (b/button
         {:bs-style "warning"
          :class "pull-right"
          :bs-size "xsmall"
          :on-click
          #(vms/make-network-primary uuid mac)} "!"))
      (b/button
       {:bs-style "primary"
        :class "pull-right"
        :bs-size "xsmall"
        :on-click
        #(vms/delete-network uuid mac)} "X")]
     :list-group
     (d/ul {:class "list-group"}
           (group-li "Tag: "     tag)
           (group-li "IP: "      ip)
           (group-li "Netmask: " netmask)
           (group-li "Gateway: " gateway)
           (group-li "MAC: "     mac))})))


(defn render-networks [app owner {uuid :uuid}]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [data (get-in app [root :elements uuid])
            nets (vals (get-in app [:networks :elements]))]
        (let [networks (get-in data [:config :networks])
              rows (partition 4 4 nil networks)]
          (r/well
           {}
           (grid-row
            (g/col
             {:xs 4}
             (i/input
              {:type "select" :include-empty true :id "net-add"}
              (d/option)
              (map #(d/option {:value (:uuid %)} (:name %)) nets)))
            (g/col
             {:xs 2}
             (b/button {:bs-style "primary"
                        :on-click #(vms/add-network uuid (val-by-id "net-add"))} "Add")))
           (let [render-network (partial render-network uuid)]
             (g/grid
              nil
              (map #(g/row nil (map render-network %)) rows)))))))))

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
        vm (:uuid element)
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
              ["Name" "CPU" "Memory" "Quota" ""]))

        (apply d/tbody
               {}
               (map
                (fn [[uuid {name :name :as pkg}]]
                  (let [cmp #(let [v (if package (package %1) 0)]
                               (cond
                                 (> %2 v) (r/glyphicon {:glyph "chevron-up"})
                                 (< %2 v) (r/glyphicon {:glyph "chevron-down"})
                                 :else ""))
                        td (fn [v f] (d/td (f (pkg v)) (cmp v (pkg v))))
                        current (= uuid current-package)]
                    (d/tr
                     {:class (if current "current" "")
                      :on-mouse-over (fn [e] (set-state! [:tmp :pkg] pkg))
                      :on-mouse-leave (fn [e] (set-state! [:tmp :pkg] {}))}
                     (d/td name)
                     (td :cpu_cap fmt-percent)
                     (td :ram     #(fmt-bytes :mb %))
                     (td :quota   #(fmt-bytes :gb %))
                     (d/td (if (not current)
                             (r/glyphicon {:glyph "transfer"
                                           :on-click #(vms/change-package vm uuid)}))))))
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
                      ["Roll Back" #(vms/restore-snapshot vm uuid)]
                      ["Delete"    #(vms/delete-snapshot vm uuid)])))))

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
                             :on-click (fn []
                                         (if (not (empty? (val-by-id "snapshot-comment")))
                                           (vms/snapshot (:uuid data) (val-by-id "snapshot-comment"))))} "Create")))))
        (snapshot-table (:uuid data) (:snapshots data)))))))

(defn backup-row  [vm [uuid {comment :comment timestamp :timestamp
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
                      ["Incremental" #(vms/backup vm uuid (val-by-id "backup-comment"))]
                      ["Roll Back" #(vms/restore-backup vm uuid)]

                      ["Delete"    #(vms/delete-backup vm uuid)])))))

(defn backup-table [vm backups]
  (g/col
   {:md 11}
   (table
    {:id "backup-table"}
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
            (partial backup-row vm)
            (sort-by (fn [[_ {t :timestamp}]] t) backups))))))

(defn render-backups [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (grid-row
        (g/col
         {:md 12}
         (i/input
          {:label "New Backup"}
          (grid-row
           (g/col
            {:xs 10}
            (i/input {:type :text
                      :placeholder "Backup Comment"
                      :id "backup-comment"}))
           (g/col {:xs 2}
                  (b/button {:bs-style "primary"
                             :wrapper-classname "col-xs-2"
                             :on-click (fn []
                                         (if (not (empty? (val-by-id "backup-comment")))
                                           (vms/backup (:uuid data) (val-by-id "backup-comment"))))} "Create")))))
        (backup-table (:uuid data) (:backups data)))))))


(defn render-fw-rules [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str (:fw_rules data))))))

(defn nice-metrics [metrics]
  (reduce #(assoc %1 (:n %2) (:v %2)) metrics))

(defn render-metrics [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (r/well
       {}
       (pr-str (nice-metrics (:metrics data)))))))

(defn b [f]
  #(om/build f %2))

(def sections
  {""          {:key  1 :fn (b render-home)      :title "General"}
   "networks"  {:key  2 :fn #(om/build
                              render-networks %1
                              {:opts {:uuid (:uuid %2)}})  :title "Networks"}
   "package"   {:key  3 :fn render-package   :title "Package"}
   "snapshots" {:key  4 :fn (b render-snapshots) :title "Snapshot"}
   "backups"   {:key  5 :fn (b render-backups)   :title "Backups"}
   "services"  {:key  6 :fn #(om/build services/render %2   {:opts {:action vms/service-action}})  :title "Services"}
   "logs"      {:key  7 :fn (b render-logs)      :title "Logs"}
   "fw-rules"  {:key  8 :fn (b render-fw-rules)  :title "Firewall"}
   "metrics"   {:key  9 :fn (b render-metrics)   :title "Metrics"}
   "metadata"  {:key 10 :fn (b metadata/render)  :title "Metadata"}})
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

(def render
  (view/make
   root sections
   (fn [data uuid]
     ;;TODO: Make sure to re-enable this!
     #_(start-timer! (get-in data [root :selected]))
     (networks/list data)
     (vms/get uuid))))
