(ns jingles.fields)

(def defaults
     {:uuid {:no-quick-filter true :class "uuid"}})

(def default-fields
  {:uuid {:title "UUID" :key :uuid :type :uuid :order -1}
   })

(defn apply-defaults
  ([fields]
   (let [fields (merge default-fields fields)]
     (reduce
      (fn [acc id]
        (assoc acc id  (apply-defaults id (fields id))))
      {} (keys fields))))
  ([id {type :type :as field}]
   (merge (get-in defaults [type] {}) (assoc field :id id))))


(defn mk-config [root title actions & {:as fields}]
  (let [conf {:title title
              :root root
              :fields (apply-defaults fields)}]
    (if actions
      (assoc conf :actions actions)
      conf)))

;:actions
;{:style {:width "20px"}}
