(ns jingles.fields
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core.match]))

(def all-defaults
  {:formater str :type :string})

(defn fmt-percent [p]
  (str p "%"))

(defn byte-suffix [type]
  (condp = type
    :b "B"
    :kb "KB"
    :mb "MB"
    :gb "GB"
    :tb "TB"
    :pb "PB"
    "holy shit!"))

(def next-byte-type
  {:b  :kb
   :kb :mb
   :mb :gb
   :gb :tb
   :tb :pb})

(def prev-byte-type
  {:bk :b
   :mb :kb
   :gb :mb
   :tb :gb
   :pb :bt})

(defn fmt-bytes [type size]
  (if (and (> size 1024) (next-byte-type type))
    (fmt-bytes (next-byte-type type)  (/ size 1024))
    (str (clojure.string/replace (.toFixed size 2) #"\.?0*$" "") (byte-suffix type))))

(defn type-defaults [type]
  (match
   type
   :uuid {:no-quick-filter true :class "uuid"}
   :percent {:formater fmt-percent}
   [:bytes size] {:formater (partial fmt-bytes size)}
   :else {}))

(def default-fields
  {:uuid {:title "UUID" :key :uuid :order -10  :type :uuid}
   :name {:title "Name" :key :name :order -20}})

(defn apply-defaults
  ([fields]
   (let [fields (merge default-fields fields)]
     (reduce
      (fn [acc id]
        (assoc acc id (apply-defaults id (fields id))))
      {} (keys fields))))
  ([id {type :type :as field}]
   (merge all-defaults (type-defaults type) (assoc field :id id))))


(defn mk-config [root title actions & {:as fields}]
  (let [conf {:title title
              :root root
              :fields (apply-defaults fields)}]
    (if actions
      (assoc conf :actions actions)
      conf)))

;:actions
;{:style {:width "20px"}}
