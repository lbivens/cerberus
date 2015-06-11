(ns jingles.hypervisors.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [jingles.utils :refer [goto grid-row display]]
   [jingles.http :as http]
   [jingles.api :as api]
   [jingles.hypervisors.api :as hypervisors]
   [jingles.hypervisors.api :refer [root]]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [fmt-bytes fmt-percent]]))

(enable-console-print!)

(defn apply-fmt [fmt v & rest]
  (concat [(fmt v)] rest))

(defn info [osname osver chunterversion boottime]
   (d/div
     (p/panel 
       {:header (d/h3 "Info")
        :list-group(d/ul {:class "list-group"}
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Operating System")
           (d/div {:class "span-value"} osname))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "OS Version")
           (d/div {:class "span-value"} osver))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Chunter Version")
           (d/div {:class "span-value"} chunterversion))
          (d/li {:class "list-group-item"}
           (d/span {:class "span-label"} "Last Boot")
           (d/div {:class "span-value"}  (.toISOString (js/Date. (* boottime 1000))))))}))
)

(defn hardware [cpu cores virt_support mainboard manufacturer serial_number]
   (d/div
     (p/panel 
       {:header (d/h3 "Hardware")
        :list-group(d/ul {:class "list-group"}
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "CPU")
           (d/div {:class "span-value"} cpu))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Cores")
           (d/div {:class "span-value"} cores))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Mainboard")
           (d/div {:class "span-value"} mainboard))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Manufacturer")
           (d/div {:class "span-value"} manufacturer))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Serial Number")
           (d/div {:class "span-value"} serial_number))
          (d/li {:class "list-group-item"}
           (d/div {:class "span-label"} "Virtualisation Support")
           (for  [virt_support_type virt_support]
             (d/div {:class "span-value"} (str virt_support_type))
           )))})))

(defn memory [total provisioned free reserved l1size l1hit]
 (d/div
   (p/panel 
     {:header (d/h3 "Memory")
      :list-group(d/ul {:class "list-group"}
        (d/li {:class "list-group-item"}
         (d/div {:class "span-label"} "Total")
         (d/div {:class "span-value"} total))
        (d/li {:class "list-group-item"}
         (d/div {:class "span-label"} "Provisioned")
         (d/div {:class "span-value"} provisioned))
        (d/li {:class "list-group-item"}
         (d/div {:class "span-label"} "Free")
         (d/div {:class "span-value"} free))
        (d/li {:class "list-group-item"}
         (d/div {:class "span-label"} "Reserved")
         (d/div {:class "span-value"} reserved))
        (d/li {:class "list-group-item"}
         (d/div {:class "span-label"} "L1 Cache Size")
         (d/div {:class "span-value"} l1size))
        (d/li {:class "list-group-item"}
         (d/div {:class "span-label"} "L1 Cache Hit %")
         (d/div {:class "span-value"} l1hit)))})))
        
(defn storage [pools disks]
 (d/div
   (p/panel 
     {:header (d/h3 "Storage")
      :list-group(d/ul {:class "list-group"}
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
             pools))
      )})))


(defn render-home [app element] 
  (let [pools (:pools element)
       sysinfo (:sysinfo element)
       bootparams ((keyword "Boot Parameters") sysinfo)
       resources (:resources element)   
       osname (cond
                (if (:smartos bootparams) #{"true"}) "SmartOS"
               ;(true? (:omnios bootparams)) "OmniOS"
                :else "Unknown")]
    (r/well
       {}
       (grid-row 
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
                     ((keyword "Product") sysinfo)
                     ((keyword "Manufacturer") sysinfo)
                     ((keyword "Serial Number") sysinfo))))
      (grid-row
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
          (storage pools, ((keyword "Disks") sysinfo)) 
        ) 
        (g/col
          {:md 4}
          "Networks")))))

(defn render-perf [app element] 
  "stub"  
)

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

(defn render-chars [app element] 
  "stub"  
)
(defn render-notes [app element] 
  "stub"  
)
(defn render-metadata [app element] 
  "stub"  
)


(def sections {""          {:key  1 :fn render-home      :title "General"}
               "perf"      {:key  2 :fn render-perf      :title "Performance"}
               "services"  {:key  3 :fn render-services  :title "Services"}
               "chars"     {:key  4 :fn render-chars     :title "Characteraristics"}
               "notes"     {:key  5 :fn render-notes     :title "Notes"}
               "metadata"  {:key  6 :fn render-metadata  :title "Metadata"}})



(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "hyperview")
    om/IWillMount
    (will-mount [_]
      (hypervisors/get (get-in data [root :selected]))
      )
    om/IInitState
    (init-state [_]
      {:edit-alias false}
      )
    om/IRenderState
    (render-state [_ state]
      (let [uuid (get-in data [root :selected])
        element (get-in data [root :elements uuid])
        sysinfo (:sysinfo element)
        bootparams ((keyword "Boot Parameters") sysinfo)
        section (get-in data [root :section])
        key (get-in sections [section :key] 1)   
        oslogo (cond
                  (if (:smartos bootparams) #{"true"}) "/imgs/smartos-stacked-logo.png"
                  ;(true? (:omnios bootparams)) "OmniOS"
                  :else "Unknown")]

        (d/div
          {}
          (grid-row 
            (g/col
            {:md 9} 
              (d/h1 
                {:style (display (not (:edit-alias state)))
                 :onClick #((om/set-state! owner :edit-alias true)
                            (om/set-state! owner :edit-alias-value (:alias element)))}
                (:alias element) " "
                )
              (d/input 
                {:style (display (:edit-alias state))
                 :value (:edit-alias-value state)
                 :onChange (fn [e] (om/set-state! owner :edit-alias-value (.. e -target -value)))
                 :onKeyDown #(when (= (.-key %) "Enter")
                                (hypervisors/rename uuid (:edit-alias-value state))
                                (om/set-state! owner :edit-alias false))
                  :onBlur (fn [e]
                            (when (:edit-alias state)
                              (hypervisors/rename uuid (:edit-alias-value state))
                                (om/set-state! owner :edit-alias false)))
                 })
              (d/h6 uuid))
            (g/col
              {:md 3}
              (dom/img #js {:src oslogo
                            :className "os-logo"})
              )) 
          (apply n/nav {:bs-style "tabs" :active-key key}
                (map
                 (fn [[section data]]
                   (n/nav-item {:key (:key data)
                                :href (str "#/hypervisors/" uuid (if (empty? section) "" (str "/" section)))}
                               (:title data)))
                 (sort-by (fn [[section data]] (:key data)) (seq sections))))
          (if-let [f (get-in sections [section :fn] )]
           (f data element)
           (goto (str "/hypervisors/" uuid)))
           ;(render-home data element)
        )))))