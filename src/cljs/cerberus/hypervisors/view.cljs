(ns cerberus.hypervisors.view
  (:require-macros [cljs.core.match.macros :refer [match]])
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
   [cerberus.utils :refer [goto row display ->state]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.hypervisors.api :as hypervisors :refer [root]]
   [cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state! app-state]]
   [cerberus.view :as view]
   [cerberus.metrics :as metrics]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn apply-fmt [fmt v & rest]
  (concat [(fmt v)] rest))

(defn li [[label value]]
  (d/li {:class "list-group-item"}
        (d/div {:class "span-label"} label)
        (d/div {:class "span-value"} value)))

(defn lg [& items]
  (d/ul
   {:class "list-group"}
   (map li (partition 2 items))))

(defn info [osname osver chunterversion boottime]
  (d/div
   (p/panel
    {:header (d/h3 "Info")
     :list-group
     (lg
      "Operating System" osname
      "OS Version"       osver
      "Chunter Version"  chunterversion
      "Last Boot"        (.toISOString (js/Date. (* boottime 1000))))})))

(defn hardware [cpu cores virt_support mainboard manufacturer serial_number]
  (d/div
   (p/panel
    {:header (d/h3 "Hardware")
     :list-group
     (lg
      "CPU"                    cpu
      "Cores"                  cores
      "Mainboard"              mainboard
      "Manufacturer"           manufacturer
      "Serial Number"          serial_number
      "Virtualisation Support" (clojure.string/join ", " virt_support))})))

(defn memory [total provisioned free reserved l1size l1hit]
  (d/div
   (p/panel
    {:header (d/h3 "Memory")
     :list-group
     (lg
      "Total"          total
      "Provisioned"    provisioned
      "Free"           free
      "Reserved"       reserved
      "L1 Cache Size"  l1size
      "L1 Cache Hit %" l1hit)})))

(defn storage [pools disks]
  (d/div
   (p/panel
    {:header (d/h3 "Storage")
     :list-group
     (d/ul {:class "list-group"}
           (d/li {:class "list-group-item"}
                 (d/b {:class "span-label"} "Disks")
                 (map (fn [[disk disk-info]]
                        [(d/div {:class "span-value"}
                                (clojure.string/replace (str disk) #"^:" "") ": "
                                ((keyword "Size in GB") disk-info))])
                      disks))
           (d/li {:class "list-group-item"}
                 (d/b {:class "span-label"} "Pools")
                 (map (fn [[pool pool-info]]
                        [(d/div {:class "span-value"}
                                (d/b (d/i (clojure.string/replace (str pool) #"^:" "") ": "))
                                (d/br)
                                "Health: "
                                (:health pool-info)
                                (d/br)
                                "Size: "
                                (apply-fmt (partial fmt-bytes :mb) (:size pool-info))
                                (d/br)
                                "Free: "
                                (apply-fmt (partial fmt-bytes :mb) (:free pool-info))
                                (d/br)
                                "Used: "
                                (apply-fmt (partial fmt-bytes :mb) (:used pool-info))
                                ) ])
                      pools)))})))

(defn render-home [element owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "hypervisor-home")
    om/IInitState
    (init-state [_]
      {:alias (:alias element)})

    om/IRenderState
    (render-state [_ state]
      (let [pools (:pools element)
            uuid (:uuid element)
            sysinfo (:sysinfo element)
            bootparams ((keyword "Boot Parameters") sysinfo)
            resources (:resources element)
            osname (cond
                     (= (:smartos bootparams) "true") "SmartOS"
                     ;;(true? (:omnios bootparams)) "OmniOS"
                     :else "Unknown")]
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
             :className "pull-right"
             :on-click #(hypervisors/set-config uuid {:alias (:alias state)})
             :disabled? (empty? (:alias state))}
            "Change alias")))
         (row
          (g/col
           {:md 6}
           (info osname
                 ((keyword "Live Image") sysinfo)
                 (:version element)
                 (int ((keyword "Boot Time") sysinfo))))
          (g/col
           {:md 6}
           (hardware ((keyword "CPU Type") sysinfo)
                     ((keyword "CPU Total Cores") sysinfo)
                     (:virtualisation element)
                     (:Product sysinfo)
                     (:Manufacturer sysinfo)
                     ((keyword "Serial Number") sysinfo))))
         (row
          (g/col
           {:md 4}
           (memory (:total-memory resources)
                   (:provisioned-memory resources)
                   (:free-memory resources)
                   (:reserved-memory resources)
                   (:l1size resources)
                   (/ (:l1hits resources) (+ (:l1hits resources) (:l1miss resources)) .01)))
          (g/col
           {:md 4}
           (storage pools (:Disks sysinfo)))
          (g/col
           {:md 4}
           (p/panel {:header "Networks"}))))))))

(defn render-perf [app element]
  "stub"
  )

(defn render-chars [app element]
  "stub"
  )

(defn render-notes [app element]
  "stub"
  )

(defn build-metric [acc {name :name points :points}]
  (match
   [name]

   [["cpu" sub-metric]]
   (assoc-in acc ["CPU" sub-metric] points)

   [_] acc))

(def sections
  {""          {:key  1 :fn  #(om/build render-home %2)     :title "General"}
   "metrics"   {:key  9 :fn #(om/build metrics/render (:metrics %2) {:opts {:translate build-metric}})   :title "Metrics"}
   "services"  {:key  3 :fn #(om/build services/render %2   {:opts {:action hypervisors/service-action}})  :title "Services"}
   "chars"     {:key  4 :fn render-chars     :title "Characteraristics"}
   ;"notes"     {:key  5 :fn render-notes     :title "Notes"}
   "metadata"  {:key  6 :fn #(om/build metadata/render %2)  :title "Metadata"}})


(def logo
  {:smartos "/imgs/smartos-stacked-logo.png"
   :other "/imgs/unknown-logo.png"})

;; This is really ugly but something is crazy about the reify for OM here
;; this for will moutnt and will unmoutn are not the same and having timer in
;; let does not work either so lets "MAKE ALL THE THINGS GLOBAL!"

(def timer (atom))

(defn stop-timer! []
  (if @timer
    (js/clearInterval @timer))
  (reset! timer nil))

(defn tick [uuid]
  (if (and
       (= (get-in @app-state [root :selected]) uuid)
       (= (:section @app-state) :hypervisors))
    (hypervisors/metrics uuid)
    (stop-timer!)))

(defn start-timer! [uuid]
  (stop-timer!)
  (reset! timer (js/setInterval #(tick uuid) 1000)))

(def render
  (view/make
   root sections
   hypervisors/get
   :init-state {:edit-alias false}
   :mount-fn (fn [uuid data]
               (start-timer! uuid))
   :name-fn (fn [element]
              (let [sysinfo (:sysinfo element)
                    bootparams ((keyword "Boot Parameters") sysinfo)
                    os (cond
                         (= (:smartos bootparams) "true") :smartos
                         :else :other)]
                (d/div
                 {}
                 (:alias element)
                 (d/img {:src (logo os)
                         :alt (name os)
                         :className "os-logo"}))))))
