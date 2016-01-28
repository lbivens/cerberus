(ns cerberus.vms.view
  (:require-macros [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [clojure.string :as cstr]
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [om-bootstrap.button :as b]
   [cerberus.utils :refer [lg goto grid-row row ->state val-by-id str->int]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.orgs.api :as orgs]
   [cerberus.hypervisors.api :as hypervisors]
   [cerberus.datasets.api :as datasets]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.vms.api :refer [root] :as vms]
   [cerberus.networks.api :as networks]
   [cerberus.view :as view]
   [cerberus.packages.api :as packages]
   [cerberus.state :refer [app-state set-state!]]
   [cerberus.fields :as fields]
   [cerberus.metrics :as metrics]
   [cerberus.utils :refer [make-event menu-items]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(def token-path "sessions/one_time_token")

(defn open-with-ott [path]
  (go
    (let [response (<! (http/get token-path))]
      (if (= 200 (:status response))
        (let [ott (get-in response [:body :token])]
          (.open js/window (str path "&ott=" ott)))))))

(def sub-element (partial api/get-sub-element))

(defn get-package [element]
  (sub-element :packages :package [:name] element))

(defn get-dataset [element]
  (sub-element :datasets :dataset [:name] element))

(defn render-home [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [uuid (get-in app [root :selected])]
        {:org (or (first (first (get-in app [:orgs :elements]))) "")
         :alias (get-in app [root :elements uuid :config :alias])}))
    om/IRenderState
    (render-state [_ state]
      (let [uuid (get-in app [root :selected])
            element (get-in app [root :elements uuid])
            conf (:config element)
            current-owner (:owner element)
            invalid-owner #{"" current-owner}
            orgs (get-in app [:orgs :elements])
            org (api/get-sub-element :orgs :owner identity element)
            package (api/get-sub-element :packages :package identity element)
            dataset (api/get-sub-element :datasets :dataset identity element)
            hypervisor (api/get-sub-element :hypervisors :hypervisor identity element)
            services (:services element)]
        (r/well
         {}
         (row
          (g/col
           {:md 8}
           (i/input
            {:type "text"
             :value (:alias state)
             :on-change (->state owner :alias)}))
          (g/col
           {:md :4}
           (b/button
            {:bs-style "primary"
             :className "pull-right fbutown"
             :on-click #(vms/change-alias uuid (:alias state))
             :disabled? (empty? (:alias state))}
            "change-alias")))
         (row
          (g/col
           {:md 8}
           (i/input
            {:type "select"
             :value (:org state)
             :on-change (->state owner :org)}
            (map (fn [[uuid e]] (d/option {:value uuid} (:name e))) orgs)))
          (g/col
           {:md :4}
           (b/button
            {:bs-style "primary"
             :className "pull-right fbutown"
             :on-click #(vms/set-owner uuid (:org state))
             :disabled? (invalid-owner (:org state))}
            "Set owner")))
         (row
          (g/col
           {:sm 6 :md 4}
           (p/panel
            {:header (d/h3 "General")
             :list-group
             (lg
              "UUID"       uuid
              "Type"       (:type conf)
              "Alias"      (:alias conf)
              "State"      (:state conf)
              "Created"    (:created_at conf)
              "Hypervisor" (d/a {:href (str "#/hypervisors/" (:uuid hypervisor))} (:alias hypervisor))
              "Owner"      (d/a {:href (str "#/orgs/" (:uuid org))} (:name org))
              "Autoboot"   (:autoboot conf)
              "Dataset"    (d/a {:href (str "#/datasets/" (:uuid dataset))} (:name dataset))
              "Package"    (d/a {:href (str "#/packages/" (:uuid package))} (:name package))
              "Services" (d/span (count (filter (fn [[_ state]] (= state "maintenance")) services)) "/"
                                 (count (filter (fn [[_ state]] (= state "online")) services)) "/"
                                 (count (filter (fn [[_ state]] (= state "disabled")) services))))}))
          (g/col
           {:sm 6 :md 4}
           (p/panel
            {:header (d/h3 "CPU / Memory")
             :list-group
             (lg
              "CPU Shares" (:cpu_shares conf)
              "CPU Cap"    (-> (:cpu_cap conf) fmt-percent)
              "Max Swap"   (->> (:max_swap conf) (fmt-bytes :b))
              "Memory"     (->> (:ram conf) (fmt-bytes :mb)))}))
          (g/col
           {:sm 6 :md 4}
           (p/panel
            {:header (d/h3 "Disk")
             :list-group
             (lg
              "Quota"         (->> (:quota conf) (fmt-bytes :gb))
              "I/O Priority"  (:zfs_io_priority conf)
              "Backups"       (count (:backups element))
              "Snapshots"     (count (:snapshots element)))}))
          (g/col
           {:sm 6 :md 4}
           (p/panel
            {:header (d/h3 "Networking")
             :list-group
             (lg
              "Hostname"       (:hostname conf)
              "DNS Domain"     (:dns_domain conf)
              "Resolvers"      (cstr/join ", " (:resolvers conf))
              "Firewall Rules" (count (:fw_rules conf))
              "IPs" (cstr/join ", " (map :ip (:networks conf))))}))))))))

(defn render-imaging [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {})
    om/IRenderState
    (render-state [_ state]
      (let [invalid (or
                     (empty? (:name state))
                     (empty? (:version state))
                     (empty? (:os state))
                     (empty? (:desc state)))]
        (r/well
         {}
         (g/row
          {}
          (g/col
           {}
           (d/p
            "To create a new image follow this steps:"
            (d/ol
             (d/li "Make sure everything is working fine on the vm")
             (d/li "Execute sm-prepare-image to make the vm image-ready")
             (d/li "Create a snapshot of the vm")
             (d/li "Set the data of the image, filling the form")
             (d/li "Choose the snapshot you want to base the image on"))
            "Then, wait until the image is ready, the datasets page will reflect that state. After that, a new vm could be created with the new image. More info " (d/a {:href "#"} "here") "."
            (pr-str ))))
         (g/row
          {}
          (g/col
           {:xs 8}
           (i/input {:type "text" :placeholder "Name"
                     :value (:name state) :on-change (->state owner :name)}))
          (g/col
           {:xs 2}
           (i/input {:type "text" :placeholder "Version"
                     :value (:version state) :on-change (->state owner :version)}))
          (g/col
           {:xs 2}
           (i/input {:type "text" :placeholder "OS"
                     :value (:os state) :on-change (->state owner :os)}))
          (g/col
           {:xs 12}
           (i/input {:type "text" :placeholder "Description"
                     :value (:desc state) :on-change (->state owner :desc)})))
         (g/row
          {}
          (g/col
           {}
           (table
            {}
            (d/thead
             (d/tr
              (d/th "Name")
              (d/th "Date")
              (d/th "Size")
              (d/th "")))
            (d/tbody
             (map (fn [[uuid {comment :comment timestamp :timestamp size :size}]]
                    (d/tr
                     (d/td comment)
                     (d/td (str (js/Date. (/ timestamp 1000))))
                     (d/td (fmt-bytes :b size))
                     (d/td (b/button
                            {:bs-style "primary"
                             :bs-size "small"
                             :className "pull-right fbutown"
                             :on-click #(datasets/from-vm (:uuid data) uuid (:name state) (:version state) (:os state) (:descs state))
                             :disabled? invalid}
                            "Create Image"))))
                  (filter
                   #(= "completed" (:state (second %)))
                   (sort-by #(:timestamp (second %)) (:snapshots data)))))))))))))

(defn render-logs [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [logs (:log data)]
        (r/well
         {}
         (table
          {:striped? true :condensed? true :hover? true :responsive? true :class "logs-list"}
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
               (d/td (str (js/Date. (/ date 1000))))
               (d/td log)))
            logs))))))))

(defn group-li [& args]
  (d/li {:class "list-group-item"} args))

(defn render-network
  [uuid disabled
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
          :bs-size "small"
          :disabled? disabled
          :on-click
          #(vms/make-network-primary uuid mac)} (r/glyphicon {:glyph "check"})))
      (b/button
       {:bs-style "primary"
        :class "pull-right"
        :bs-size "small"
        :disabled? disabled
        :on-click
        #(vms/delete-network uuid mac)} (r/glyphicon {:glyph "trash"}))]
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
            nets (sort-by :name (vals (get-in app [:networks :elements])))
            disabled (not  (#{"stopped" "installed"} (:state data)))
            networks (get-in data [:config :networks])
            rows (partition 4 4 nil networks)
            render-network (partial render-network uuid disabled)]
        (r/well
         {}
         (row
          (g/col
           {:xs 4}
           (i/input
            {:type "select" :include-empty true :id "net-add"}
            (d/option)
            (map #(d/option {:value (:uuid %)} (:name %)) nets)))
          (g/col
           {:xs 2}
           (b/button {:bs-style "primary"
                      :disabled? disabled
                      :on-click #(vms/add-network uuid (val-by-id "net-add"))} "Add")))
         (map #(g/row nil (map render-network %)) rows))))))

(defn cmp-vals [package cmp-package val fmt]
  (if-let [cmp-vap (cmp-package val)]
    (let [val (if package (package val) 0)
          diff (- cmp-vap val)]
      (cond
        (> diff 0) [(fmt val) " (+" (fmt diff) ")"]
        (< diff 0) [(fmt val) " (" (fmt diff) ")"]
        :else [(fmt  val)]))
    [(if package (package val) 0)]))

(defn apply-fmt [fmt rest-fn]
  (rest fmt))

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
     (row
      (g/col
       {:md 4}
       (p/panel
        {:header (if package (:name package) "custom")
         :list-group
         (d/ul {:class "list-group"}
               (group-li "CPU: "    (cmp-vals :cpu_cap fmt-percent))
               (group-li "Memory: " (cmp-vals :ram (partial fmt-bytes :mb)))
               (group-li "Quota: "  (partial cmp-vals :quota (partial fmt-bytes :gb))))}))
      (g/col
       {:md 8}
       (table
        {:class "pkg-list"}
        (d/thead
         {}
         (map d/td
              ["Name" "CPU" "Memory" "Quota" (d/span {:class "pull-right"} "Change")]))

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
                                           :class "pull-right"
                                           :alt "change package"
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
    om/IInitState
    (init-state [_]
      {:name ""})
    om/IRenderState
    (render-state [_ state]
      (r/well
       {}
       (row
        (g/col
         {:md 12}
         (i/input
          {:label "New Snapshot"}
          (row
           (g/col
            {:xs 10}
            (i/input {:type :text
                      :value (:name state)
                      :placeholder "Snapshot Comment"
                      :on-change (->state owner :name)}))
           (g/col
            {:xs 2}
            (b/button {:bs-style "primary"
                       :wrapper-classname "col-xs-2"
                       :disabled? (empty? (:name state))
                       :on-click #(vms/snapshot (:uuid data) (:name state))} "Create")))))
        (snapshot-table (:uuid data) (:snapshots data)))))))


(defn show-state [state]
  (condp = state
    "uploading" (r/label {:bs-style "warning"} state)
    "completed" (r/label {:bs-style "success"} state)
    "failed" (r/label {:bs-style "danger"} state)
    (r/label {:bs-style "default"} state)))

(defn backup-row  [vm hypervisor
                   [uuid {comment :comment timestamp :timestamp
                          state :state old-size :size files :files}]]
  (let [size (reduce + (map #(:size (second %)) files))
        size (if (= 0 size) old-size size)]
    (d/tr
     (d/td (name uuid))
     (d/td comment)
     (d/td (str (js/Date. (/ timestamp 1000))))
     (d/td (show-state state))
     (d/td (fmt-bytes :b size))
     (d/td {:class "actions no-carret"}
           (b/dropdown {:bs-size "xsmall" :title (r/glyphicon {:glyph "option-vertical"})
                        :on-click (make-event identity)}
                       (menu-items
                        ["Incremental" #(vms/backup vm uuid (val-by-id "backup-comment"))]
                        (if (and hypervisor (not (empty? hypervisor)))
                          ["Restore" #(vms/restore-backup vm hypervisor uuid)]
                          ["Roll Back" #(vms/restore-backup vm uuid)])

                        ["Delete"    #(vms/delete-backup vm uuid)]))))))

(defn backup-table [vm hypervisor backups]
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
            (partial backup-row vm hypervisor)
            (sort-by (fn [[_ {t :timestamp}]] t) backups))))))

(defn render-backups [app owner {:keys [uuid]}]
  (reify
    om/IInitState
    (init-state [_]
      {:name ""})
    om/IRenderState
    (render-state [_ state]
      (let [data (get-in app [root :elements uuid])]
        (r/well
         {}
         (if (not (empty? (:hypervisor data)))
           (row
            (g/col
             {:xs 12}
             (d/p
              "Once a backup is made it is possible to remove a zone from"
              " a hypervisor without deleting the backups, that way the"
              " zone can later on be deployed again."
              (b/button {:bs-style "danger"
                         :disabled? (empty? (:backups data))
                         :on-click #(vms/delete-hypervisor (:uuid data))

                         :class "pull-right"} "Delete from hypervisor"))))
           (row
            (g/col {}
                   (d/p
                    "This vm is in 'limbo' it currently has no hypervisors assigned"
                    " that means it can be re-deployed.")
                   (i/input {:type "select"
                             :value (:target state)
                             :on-change (->state owner :target)}
                            (d/option "")
                            (map
                             (fn [[h-uidd {alias :alias}]]
                               (d/option {:value h-uidd} alias))
                             (get-in app [:hypervisors :elements])))
                   )))
         (row

          (g/col
           {:md 12}
           (i/input
            {:label "New Backup"}
            (row
             (g/col
              {:xs 10}
              (i/input {:type :text
                        :placeholder "Backup Comment"
                        :on-change (->state owner :name)
                        :value (:name state)
                        :id "backup-comment"}))
             (g/col {:xs 2}
                    (b/button {:bs-style "primary"
                               :wrapper-classname "col-xs-2"
                               :disabled? (empty? (:name state))
                               :on-click #(vms/backup (:uuid data) (:name state))} "Create")))))
          (backup-table uuid (:target state) (:backups data))))))))


(defn o-state! [owner id]
  (om/set-state! owner id (val-by-id (name id))))


(def icmp
  {"0"  {:name "Echo Reply" :codes {"0" "No Code"}}
   "3"  {:name "Destination Unreachable"
         :codes {"0"  "Net Unreachable"
                 "1"  "Host Unreachable"
                 "2"  "Protocol Unreachable"
                 "3"  "Port Unreachable"
                 "4"  "Fragmentation Needed and Don't Fragment was Set"
                 "5"  "Source Route Failed"
                 "6"  "Destination Network Unknown"
                 "7"  "Destination Host Unknown"
                 "8"  "Source Host Isolated"
                 "9"  "Communication with Destination Network is Administratively Prohibited"
                 "10" "Communication with Destination Host is Administratively Prohibited"
                 "11" "Destination Network Unreachable for Type of Service"
                 "12" "Destination Host Unreachable for Type of Service"
                 "13" "Communication Administratively Prohibited"
                 "14" "Host Precedence Violation"
                 "15" "Precedence cutoff in effect"}}
   "4"  {:name "Source Quench" :codes {"0" "No Code"}}
   "5"  {:name "Redirect"
         :codes {"0" "Redirect Datagram for the Network (or subnet)"
                 "1" "Redirect Datagram for the Host"
                 "2" "Redirect Datagram for the Type of Service and Network"
                 "3" "Redirect Datagram for the Type of Service and Host"}}
   "6"  {:name "Alternate Host Address" :codes {0 "Alternate Address for Host"}}
   "8"  {:name "Echo" :codes {"0" "No Code"}}
   "9"  {:name "Router Advertisement" :codes {"0" "No Code"}}
   "10" {:name "Router Selection" :codes {"0" "No Code"}}
   "11" {:name "Time Exceeded"
         :codes {"0" "Time to Live exceeded in Transit"
                 "1" "Fragment Reassembly Time Exceeded"}}
   "12" {:name "Parameter Problem"
         :codes {"0" "Pointer indicates the error"
                 "1" "Missing a Required Option"
                 "2" "Bad Length"}}
   "13" {:name "Timestamp" :codes {"0" "No Code"}}
   "14" {:name "Timestamp Reply" :codes {"0" "No Code"}}
   "15" {:name "Information Request" :codes {"0" "No Code"}}
   "16" {:name "Information Reply" :codes {"0" "No Code"}}
   "17" {:name "Address Mask Request" :codes {"0" "No Code"}}
   "18" {:name "Address Mask Reply" :codes {"0" "No Code"}}
   "30" {:name "Traceroute" :codes {"0" "No Code"}}
   "31" {:name "Datagram Conversion Error" :codes {"0" "No Code"}}
   "32" {:name "Mobile Host Redirect" :codes {"0" "No Code"}}
   "33" {:name "IPv6 Where-Are-You" :codes {"0" "No Code"}}
   "34" {:name "IPv6 I-Am-Here" :codes {"0" "No Code"}}
   "35" {:name "Mobile Registration Request" :codes {"0" "No Code"}}
   "36" {:name "Mobile Registration Reply" :codes {"0" "No Code"}}
   "39" {:name "SKIP" :codes {"0" "No Code"}}
   "40" {:name "Photuris"
         :codes {"0" "Reserved"
                 "1" "unknown security parameters index"
                 "2" "valid security parameters, but authentication failed"
                 "3" "valid security parameters, but decryption failed"}}})

(def lc "col-xs-2  col-lg-1 col-md-1 col-sm-1")

(def wc "col-xs-10 col-lg-5 col-sm-5 col-md-5")

(defn select [id label owner state config & body]
  (let [merged-config (merge {:type "select" :id (name id) :label label
                              :value (id state) :class "input-sm"
                              :label-classname lc :wrapper-classname wc} config)
        final-config (if-let [change-fn (:on-change config)]
                       (assoc merged-config
                              :on-change #(do
                                            (o-state! owner id)
                                            (change-fn %)))
                       (assoc merged-config
                              :on-change #(o-state! owner id)))]
    (i/input final-config body)))

(defn direction-select [owner state]
  (select :direction "Direction" owner state {}
          (d/option {:value "inbound"} "Inbound")
          (d/option {:value "outbound"} "Outbound")))

(defn protocol-select [owner state]
  (select :protocol "Protocol" owner state
          {:on-change #(om/set-state! owner :icmp-type "0")}
          (d/option {:value "tcp"} "TCP")
          (d/option {:value "udp"} "UDP")
          (d/option {:value "icmp"} "ICMP")))

(defn target-select [owner state]
  (select :target
          (if (= (:direction state) "inbound")
            "Source" "Destination")
          owner state
          {:on-change #(om/set-state! owner :mask "24")}
          (d/option {:value "all"} "all")
          (d/option {:value "ip"} "IP")
          (d/option {:value "subnet"} "Subnet")))

(defn target-data [owner state]
  (condp = (:target state)
    "ip" (i/input {:type "text"
                   :label (if (= (:direction state) "inbound")
                            "Source IP" "Dest IP")
                   :class "input-sm" :id "ip" :value (:ip state)
                   :label-classname lc :wrapper-classname wc
                   :on-change #(o-state! owner :ip)})
    "subnet" [(i/input {:type "text" :label "Subnet" :class "input-sm"
                        :id "subnet" :value (:subnet state)
                        :label-classname lc :wrapper-classname wc
                        :on-change #(o-state! owner :subnet)})
              (select :mask "Mask" owner state {}
                      (map #(d/option {:value %} %) (range 1 33)))]
    []))

(defn port-data [owner state]
  (if (or
       (= (:protocol state) "tcp")
       (= (:protocol state) "udp"))
    [(i/input {:type "checkbox" :label "All Ports"
               :id "all-ports"
               :checked (:all-ports state)
               :wrapper-classname (str "col-xs-offset-2 col-sm-offset-1 "
                                       "col-md-offset-1 col-lg-offset-1 "
                                       "col-xl-offset-1 " wc)
               :on-change #(om/set-state! owner :all-ports (.-checked (.-target %)))})
     (if (not (:all-ports state))
       (i/input {:type "text" :label "Ports" :class "input-sm" :id "ports"
                 :value (:ports state)
                 :on-change #(o-state! owner :ports)
                 :label-classname lc :wrapper-classname wc}))]))

(defn icmp-type-select [data owner {parent :parent}]
  (reify
    om/IRenderState
    (render-state [_ _]
      (if (= (:protocol data) "icmp")
        (apply select :icmp-type "Type" parent data
               {:on-change #(om/set-state! parent :icmp-code "0")}
               (map
                (fn [[id obj]]
                  (d/option {:value id} (str (:name obj) " (" id ")")))
                (sort-by #(str->int (first %)) icmp)))))))

(defn icmp-codes [{id :id name :name} owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (d/option
       {:value id}
       (str name " (" id ")")))))

(defn icmp-code-select [data owner {parent :parent}]
  (reify
    om/IRenderState
    (render-state [_ _]
      (if  (= (:protocol data) "icmp")
        (if-let [codes (get-in icmp [(:icmp-type data) :codes])]
          (apply select :icmp-code "Code" parent data {}
                 (om/build-all
                  icmp-codes
                  (map (fn [[id name]] {:id id :name name})
                       (sort-by #(str->int (first %)) codes))
                  {:key :id})))))))

(defn action-select [owner state]
  (select :action "Action" owner state {}
          (d/option {:value "allow"} "allow")
          (d/option {:value "block"} "block")))

(defn rule-target [state]
  (condp = (:target state)
    "all" "all"
    "ip" {:ip (:ip state)}
    "subnet" {:subnet (:subnet state) :mask (str->int (:mask state))}))

(defn rule-filter [state]
  (if (= (:protocol state) "icmp")
    [{:type (str->int (:icmp-type state))
      :code (str->int (:icmp-code state))}]
    (if (:all-ports state)
      "all"
      (map str->int (cstr/split (:ports state) #"[, ]+") ))))


;; TODO: make this properly check for va
(defn valid-rule [{action :action
                   direction :direction
                   target :target
                   protocol :protocol
                   filter :filter}]
  true)

(defn add-rule [state]
  (let [payload {:action (:action state)
                 :direction (:direction state)
                 :target (rule-target state)
                 :protocol (:protocol state)
                 :filters (rule-filter state)}]
    (if (valid-rule payload)
      (vms/add-fw-rule (:uuid state) payload))))


(defn render-target [target]
  (cond
    (= "all" target) "all"
    (:ip target)  (:ip target)
    :else (str (:subnet target) "/" (:mask target))))

(defn render-filter [filters]
  (cond
    (= "all" filters) "*"
    (:code (first  filters)) (str "ICMP("(:code (first  filters)) "/" (:type (first filters)) ")")
    :else  (cstr/join ", " filters)))

(defn render-rule [uuid
                   {id :id target :target protocol :protocol action :action
                    direction :direction filters :filters}]
  (let [target-str (str protocol "://" (render-target target))
        filters-str (render-filter filters)]
    (let [btn (b/button
               {:bs-style "warning"
                :bs-size "xsmall"
                :class "pull-right"
                :on-click #(vms/delete-fw-rule uuid id)}
               "x")
          action (if (= "allow" action)
                   (r/glyphicon {:glyph "ok"})
                   (r/glyphicon {:glyph "fire"}))]
      (if (= direction "inbound")
        (d/tr
         (d/td target-str)
         (d/td action)
         (d/td (r/glyphicon {:glyph "hdd"}) ":" filters-str)
         (d/td btn))
        (d/tr
         (d/td (r/glyphicon {:glyph "hdd"}))
         (d/td action)
         (d/td target-str ":" filters-str)
         (d/td btn))))))

(defn render-fw-rules [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:uuid (:uuid data)
       :action "block"
       :direction "inbound"
       :protocol "tcp"
       :all-ports false
       :target "all"})
    om/IRenderState
    (render-state [_ state]
      (r/well
       {}
       (row
        (g/col
         {}
         (direction-select owner state)
         (protocol-select owner state)
         (target-select owner state)
         (target-data owner state)
         (port-data owner state)
         (om/build icmp-type-select state
                   {:opts {:parent owner}
                    :react-key "icmp-type"})
         (om/build icmp-code-select state
                   {:opts {:parent owner}
                    :react-key "icmp-code"})
         (action-select owner state)))
        (row
        (g/col
         {:xs 12}
         (b/button
             {:bs-style "primary"
              :class "fwaddbtn"
              :on-click #(add-rule state)}
             "add rule")))
       (row
        (g/col
         {:xs 10
          :class "fwlegend"}
         (d/p
          (d/br)
          (r/glyphicon {:glyph "fire"}) " block"
          (r/glyphicon {:glyph "ok"}) " allow"
          (r/glyphicon {:glyph "hdd"}) " this zone")))
       (row
        (g/col
         {:xs 12 :md 6}
         (p/panel
          {:header "Inbound rules"
           :class "fwrule"}
          (table
           {}
           (d/thead
            (d/tr
             (d/th "src")
             (d/th "action")
             (d/th "dst")
             (d/th)))
           (d/tbody
            (let [rules (filter #(= (:direction %) "inbound") (:fw_rules data))]
            (map (partial render-rule (:uuid data)) rules))))))
        (g/col
         {:xs 12 :md 6}
         (p/panel
          {:header "Outbound rules"
           :class "fwrule"}
          (table
           {}
           (d/thead
            (d/tr
             (d/th "src")
             (d/th "action")
             (d/th "dst")
             (d/th )))
           (d/tbody
            (let [rules (filter #(= (:direction %) "outbound") (:fw_rules data))]
              (map (partial render-rule (:uuid data)) rules)))))))))))

(defn build-metric [acc {name :name points :points}]
  (match
   [name]

   [["cpu" sub-metric]]
   (assoc-in acc ["CPU" sub-metric] points)

   [["memory" sub-metric]]
   (assoc-in acc ["Memory" sub-metric] points)

   [["swap" sub-metric]]
   (assoc-in acc ["Swap" sub-metric] points)

   [["net" direction "kb" nic]]
   (assoc-in acc [(str nic " throughput") direction] points)

   [["net" direction "ops" nic]]
   (assoc-in acc [(str nic " OPs") direction] points)

   [["zfs" direction "kb"]]
   (assoc-in acc ["ZFS throughput" direction] points)

   [["zfs" direction "ops"]]
   (assoc-in acc ["ZFS OPs" direction] points)

   [_] acc))


(defn b [f]
  #(om/build f %2))

(def sections
  {""          {:key  1 :fn #(om/build render-home %1)      :title "General"}
   "networks"  {:key  2 :fn #(om/build
                              render-networks %1
                              {:opts {:uuid (:uuid %2)}})  :title "Networks"}
   "package"   {:key  3 :fn render-package   :title "Package"}
   "snapshots" {:key  4 :fn (b render-snapshots) :title "Snapshot"}
   "imaging"   {:key  5 :fn (b render-imaging) :title "Imaging"}
   "backups"   {:key  6 :fn #(om/build render-backups %1 {:opts {:uuid (:uuid %2)}})   :title "Backups"}
   "services"  {:key  7 :fn #(om/build services/render %2 {:opts {:action vms/service-action}})  :title "Services"}
   "logs"      {:key  8 :fn (b render-logs)      :title "Logs"}
   "fw-rules"  {:key  9 :fn (b render-fw-rules)  :title "Firewall"}
   "metrics"   {:key 10 :fn #(om/build metrics/render (:metrics %2) {:opts {:translate build-metric}})   :title "Metrics"}
   "metadata"  {:key 11 :fn (b metadata/render)  :title "Metadata"}})

;; This is really ugly but something is crazy about the reify for OM here
;; this for will moutnt and will unmoutn are not the same and having timer in
;; let does not work either so lets "MAKE ALL THE THINGS GLOBAL!"

(def timer (atom))

(defn stop-timer! []
  (if @timer
    (js/clearInterval @timer))
  (reset! timer nil))

(defn tick [uuid local-timer]
  (let [app @app-state]
    (if (or
         (= (get-in app [root :elements uuid :metrics]) :no-metrics)
         (not= (get-in app [root :selected]) uuid)
         (not= (:section app) :vms)
         (= (type (vms/metrics uuid)) cljs.core.async.impl.channels/ManyToManyChannel))
      (do
        (js/clearInterval @local-timer)
        (stop-timer!)))))

(defn start-timer! [uuid]
  (stop-timer!)
  (let [local-timer (atom)
        t (js/setInterval #(tick uuid local-timer) 1000)]
    (reset! local-timer t)
    (reset! timer t)))

(def render
  (view/make
   root sections
   vms/get
   :mount-fn (fn [uuid {:type type :as  data}]
               (start-timer! uuid)
               (orgs/list data)
               (hypervisors/list data)
               (networks/list data))
   :name-fn  (fn [{:keys [state uuid hypervisor] {alias :alias} :config}]
               (d/div
                {}
                alias " "
                (b/button-group
                 {:class "fctabuttons"}
                 (b/button
                  {:bs-size "small"
                   :bs-style "primary"
                   :on-click #(open-with-ott (str "./" (if (= type "kvm") "vnc" "console")  ".html?uuid=" uuid))
                   :disabled? (not= state "running")}
                  (r/glyphicon {:glyph "modal-window"}))
                 (b/button
                  {:bs-size "small"
                   :bs-style "primary"
                   :on-click #(vms/start uuid)
                   :disabled? (= state "running")}
                  (r/glyphicon {:glyph "play"}))
                 (b/button
                  {:bs-size "small"
                   :bs-style "primary"
                   :on-click #(vms/stop uuid)
                   :disabled? (= state "stopped")}
                  (r/glyphicon {:glyph "stop"}))
                 (b/button
                  {:bs-size "small"
                   :bs-style "primary"
                   :on-click #(vms/reboot uuid)
                   :disabled? (= state "stopped")}
                  (r/glyphicon {:glyph "refresh"}))
                 (b/button
                  {:bs-size "small"
                   :bs-style "danger"
                   :on-click #(vms/delete uuid)
                   :disabled? (= state "running")}
                  (r/glyphicon {:glyph "trash"})))))))



