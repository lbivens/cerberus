(ns cerberus.hypervisors.view
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
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
   [cerberus.utils :refer [lg goto row display ->state]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.hypervisors.api :as hypervisors :refer [root]]
   ;[cerberus.services :as services]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state! app-state]]
   [cerberus.view :as view]
   [cerberus.metrics :as metrics]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn apply-fmt [fmt v & rest]
  (concat [(fmt v)] rest))

(defn info [osname osver chunterversion boottime host]
  (p/panel
   {:header (d/h3 "Info")
    :list-group
    (lg
     "Host"             host
     "Operating System" osname
     "OS Version"       osver
     "Chunter Version"  chunterversion
     "Last Boot"        (.toISOString (js/Date. (* boottime 1000)))
     "Host"             host)}))

(defn hardware [cpu cores virt_support mainboard manufacturer serial_number]
  (p/panel
   {:header (d/h3 "Hardware")
    :list-group
    (lg
     "CPU"                    cpu
     "Cores"                  cores
     "Mainboard"              mainboard
     "Manufacturer"           manufacturer
     "Serial Number"          serial_number
     "Virtualisation Support" (cstr/join ", " virt_support))}))

(defn memory [total provisioned free reserved l1size l1hit]
  (p/panel
   {:header (d/h3 "Memory")
    :list-group
    (lg
     "Total"          total
     "Provisioned"    provisioned
     "Free"           free
     "Reserved"       reserved
     "L1 Cache Size"  l1size
     "L1 Cache Hit %" l1hit)}))

(defn storage [pools disks]
  (p/panel
   {:header (d/h3 "Storage")
    :list-group
    (d/ul {:class "list-group"}
          (d/li {:class "list-group-item"}
                (d/b {:class "span-label"} "Disks")
                (map (fn [[disk disk-info]]
                       [(d/div {:class "span-value"}
                               (cstr/replace (str disk) #"^:" "") ": "
                               ((keyword "Size in GB") disk-info))])
                     disks))
          (d/li {:class "list-group-item"}
                (d/b {:class "span-label"} "Pools")
                (map (fn [[pool pool-info]]
                       [(d/div {:class "span-value"}
                               (d/b (d/i (cstr/replace (str pool) #"^:" "") ": "))
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
                     pools)))}))

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
             :on-click #(hypervisors/set-config uuid {:alias (cstr/trim (:alias state))})
             :disabled? (empty? (:alias state))}
            "Change alias")))
         (row
          (g/col
           {:md 6}
           (info osname
                 ((keyword "Live Image") sysinfo)
                 (:version element)
                 (int ((keyword "Boot Time") sysinfo))
                 (:host element)))
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
           (p/panel
            {:header "Networks"}
            (d/ul
             (map  d/li (:networks element)))))))))))

(defn render-chars [element owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "hypervisor-characteristics")
    om/IInitState
    (init-state [_]
      {})
    om/IRenderState
    (render-state [_ state]
      (let [chars    (:characteristics element)
            uuid     (:uuid element)
            invalid? (or (empty? (:char state))  (empty? (:val state)))]
        (r/well
         {}
         (row
          (g/col
           {:md 3}
           (i/input
            {:type "text"
             :placeholder "Characteristic"
             :value (:char state)
             :on-change (->state owner :char)}))
          (g/col
           {:md 7}
           (i/input
            {:type "text"
             :placeholder "Value"
             :value (:val state)
             :on-change (->state owner :val)}))
          (g/col
           {:md 2}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :on-click #(hypervisors/set-characteristic uuid (cstr/trim (:char state)) (cstr/trim (:val state)))
             :disabled? invalid?}
            "Add Characteristics")))
         (g/row
          {}
          (g/col
           {}
           (table
            {}
            (d/thead
             (d/tr
              (d/th "Characteristic")
              (d/th "Value")
              (d/th "")))
            (d/tbody
             (map
              (fn [[c v]]
                (d/tr
                 (d/td (name c))
                 (d/td v)
                 (d/td
                  (b/button {:bs-size "xsmall"
                             :className "pull-right"
                             :on-click #(hypervisors/delete-characteristic uuid (name c))}
                            (r/glyphicon {:glyph "trash"})))))
              chars))))))))))

(defn build-metric [acc {name :name points :points}]
  (match
   [name]

   [["cpu" sub-metric]]
   (assoc-in acc ["CPU" sub-metric] {:points points :unit "cycles"})

   [_] acc))

(def sections
  {""          {:key  1 :fn  #(om/build render-home %2)     :title "General"}
   ;"services"  {:key  3 :fn #(om/build services/render %2   {:opts {:action hypervisors/service-action}})  :title "Services"}
   "chars"     {:key  4 :fn #(om/build render-chars %2)     :title "Characteristics"}
   ;; "notes"     {:key  5 :fn render-notes     :title "Notes"}
   "metrics"   {:key  5 :fn #(om/build metrics/render (:metrics %2) {:opts {:translate build-metric}})   :title "Metrics"}
   "metadata"  {:key  6 :fn #(om/build metadata/render
    (:metadata %2) {:opts {:root "hypervisors" :uuid (:uuid %2)}}) :title "Metadata"}})


(def logo
  {:smartos "/imgs/smartos-stacked-logo.png"
   :other "/imgs/unknown-logo.png"})

(defn tick [uuid local-timer]
  (let [app @app-state]
    (if (and
         (not= (get-in app [root :elements uuid :metrics]) :no-metrics)
         (= (get-in app [root :selected]) uuid)
         (= (:section app) root))
      (hypervisors/metrics uuid)
      (metrics/stop-timer! local-timer))))

(def render
  (view/make
   root sections
   hypervisors/get-page
   :init-state {:edit-alias false}
   :mount-fn (fn [uuid data]
               (metrics/start-timer! (partial tick uuid)))
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
