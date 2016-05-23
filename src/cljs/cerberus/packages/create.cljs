(ns cerberus.packages.create
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.grid :as g]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.button :as b]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [->state str->int]]
   [cerberus.packages.view :refer [build-reqs]]
   [cerberus.create :as create]))




(defn conditions []
  (concat
   [["must" "Must"]
    ["cant" "Can't"]
    ["scale" "Scale"]
    ["random" "Random"]]
   (map
    (fn [i]
      [i (str (if (< 0 i) "+") i)])
    (filter #(not= 0 %) (reverse (range -15 15))))))

(def comparers
  [[">=" ">="]
   [">" ">"]
   ["=<" "=<"]
   ["<" "<"]
   ["=:=" "=:="]
   ["=/=" "=/="]
   ["element" "element"]
   ;;["subset" "subset"]
   ;;["superset" "superset"]
   ;;["disjoint" "disjoint"]
   ])


(defn rule-type [weight]
  (cond
    (or (#{"must" "cant"} weight) (re-matches #"^\d+$" weight)) :normal
    (= weight "scale") :scale
    (= weight "random") :random
    :else false)
  )

(defn valid [& args]
  (not-any? #(or (nil? %) (empty? %)) args))

(defn convert-value [condition value]
  (if (re-matches #"^[0-9]+$" value)
    (str->int value)
    value))

(defn mk-rule [{:keys [weight attribute condition low high value]}]
  (condp = (rule-type weight)
    :scale  (if (valid attribute low high)
              {:weight "scale" :attribute attribute :low (str->int low) :high (str->int high)})
    :random (if (valid attribute low high)
              {:weight "random" :low (str->int low) :high (str->int high)})
    :normal (if (valid attribute condition value)
              {:weight weight :attribute attribute :condition condition
               :value (convert-value condition value)})
    nil))

(defn render [data owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "addpackagec")
    om/IInitState
    (init-state [_]
      {:weight "must"
       :condition "=:="
       :attribute ""
       :res ""
       :res-val ""})
    om/IRenderState
    (render-state [_ state]
      (d/div
       (create/render
        data
        {:label "Name"                     :id "pkg-name"        :key :name}
        {:label "CPU"         :unit "%"    :id "pkg-cpu"         :key :cpu_cap         :data-type :integer                :validator #(and (integer? %2) (< 0 %2))}
        {:label "Memory"      :unit "MB"   :id "pkg-ram"         :key :ram             :data-type :integer                :validator #(and (integer? %2) (< 0 %2))}
        {:label "Disk"        :unit "GB"   :id "pkg-quota"       :key :quota           :data-type :integer                :validator #(and (integer? %2) (< 0 %2))}
        {:label "IO Priority"              :id "pkg-iopriority"  :key :zfs_io_priority :data-type :integer :optional true}
        {:label "Block Size"  :unit "Byte" :id "pkg-block_size"  :key :blocksize       :data-type :integer :optional true}
        {:label "Compression"              :id "pkg-compression" :key :compression                         :optional true
         :type :select :options ["lz4" "lzjb" "zle" "gzip"]})
       (g/row
        {}
        (g/col
         {:xs 12}
         (d/h4 "Rules")))
       (g/row
        {}
        (g/col
         {:sm 2}
         (i/input {:type "select"
                   :value (:weight state)
                   :on-change (->state owner :weight)}
                  (map
                   (fn [[v n]]
                     (d/option {:value v} n))
                   (conditions))))
        (if (not= "random" (:weight state))
          (g/col
           {:sm 4}
           (i/input {:type "text" :value (:attribute state) :placeholder "Attribute"
                     :on-change (->state owner :attribute)})))
        (if (not (#{"random" "scale"} (:weight state)))

          (g/col
           {:sm 2}
           (i/input {:type "select"
                     :value (:condition state)
                     :on-change (->state owner :condition)}
                    (map
                     (fn [[v n]]
                       (d/option {:value v} n))
                     comparers))))
        (if (not (#{"random" "scale"} (:weight state)))
          (g/col
           {:sm 2}
           (i/input {:type "text" :value (:value state) :placeholder "Value"
                     :on-change (->state owner :value)})))
        (if (#{"random" "scale"} (:weight state))
          (g/col
           {:sm 2}
           (i/input {:type "text" :value (:low state) :placeholder "Low"
                     :on-change (->state owner :low)})))
        (if (#{"random" "scale"} (:weight state))
          (g/col
           {:sm 2}
           (i/input {:type "text" :value (:high state) :placeholder "High"
                     :on-change (->state owner :high)})))
        (let [rule (mk-rule state)]
          (g/col
           {:sm 2}
           (b/button
            {:bs-style "primary"
             :on-click #(om/transact! data [:data :requirements] (fn [ds] (conj ds rule)))
             :disabled? (nil? rule)}
            "Add"))))
       (g/row
        {}
        (g/col
         {:sm 12}
         (build-reqs (get-in data [:data :requirements]))))
       (g/row
        {}
        (g/col
         {:xs 12}
         (d/h4 "Org Resources")))
       (g/row
        {}
        (g/col
         {:sm 6}
         (i/input {:type "text" :value (:res state) :placeholder "Resource"
                   :on-change (->state owner :res)}))
        (g/col
         {:sm 4}
         (i/input {:type "text" :value (:res-val state) :placeholder "Required"
                   :on-change (->state owner :res-val)}))
        (g/col
         {:sm 2}
         (b/button
          {:bs-style "primary"
           :on-click #(om/update! data [:data :org (:res state)] (str->int (:res-val state)))
           :disabled? (or (empty? (:res state)) (empty? (:res-val state)))}
          "Add")))
       (g/row
        {}
        (g/col
         {:sm 12}
         (table
          {:condensed? true}
          (d/thead
           (d/tr
            (d/th "Resource")
            (d/th "Value")))
          (d/tbody
           (map (fn [[r v]]
                  (d/tr
                   (d/td r)
                   (d/td v)))
                (get-in data [:data :org]))))))))))
