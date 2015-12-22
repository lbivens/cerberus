(ns cerberus.fields
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core.match]))

(def all-defaults
  {:formater str :type :string})

(defn fmt-percent [p]
  (str p "%"))

(defn fmt-percent-float [p]
  (str (Math/round (* p 100)) "%"))

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

(defn fmt-time [res timestamp]
  (if (= 0 timestamp)
    "-"
    (condp = res
      :us (str (js/Date. (/ timestamp 1000)))
      :s (str (js/Date. (* timestamp 1000)))
      (str (js/Date. timestamp)))))

(defn format-delta-w [w]
  (str (Math/round w) "w"))

(defn format-delta-d [d]
  (if (< d 7)
    (str (Math/round d) "d")
    (format-delta-w (/ d 7))))

(defn format-delta-h [h]
  (if (< h 24)
    (str (Math/round h) "h")
    (format-delta-d (/ h 24))))

(defn format-delta-m [m]
  (if (< m 60)
    (str (Math/round m) "m")
    (format-delta-h (/ m 60))))

(defn format-delta-s [s]
  (if (< s 60)
    (str (Math/round s) "s")
    (format-delta-m (/ s 60))))

(defn fmt-ago [res timestamp]
  (if (= 0 timestamp)
    "-"
    (let [ts0 (condp = res
                :us  (/ timestamp 1000)
                :s   (* timestamp 1000)
                timestamp)
          now (.getTime (js/Date.))
          ts ts0
          delta (- now ts)]
      (format-delta-s (/ delta 1000)))))

(defn fmt-bytes [type size]
  (if (not size)
    "0"
    (if (and (> size 1024) (next-byte-type type))
      (fmt-bytes (next-byte-type type)  (/ size 1024))
      (str (clojure.string/replace (.toFixed size 2) #"\.?0*$" "") (byte-suffix type)))))

(defn int-ip [ip]
  (reduce
   #(+(* 255 %1 ) %2) 0
   (map js/parseInt (clojure.string/split ip #"\."))))

(defn type-defaults [type field]
  (match
   type
   :uuid           {:no-quick-filter true :class "uuid" :show false}
   :percent        {:formater fmt-percent}
   :percent-flt    {:formater fmt-percent-float}
   [:bytes size]   {:formater (partial fmt-bytes size)}
   :ip             {:sort-key #(int-ip ((:key field) %))}
   [:timstamp res] {:formater (partial fmt-time res)
                    :sort-key identity}
   [:ago res]      {:formater (partial fmt-ago res)
                    :sort-key identity}
   :else           {}))

(def default-fields
  {:uuid {:title "UUID" :key :uuid :order -12  :type :uuid :show false}
   :name {:title "Name" :key :name :order -20}})

(defn apply-defaults
  ([fields]
   (let [fields (merge default-fields fields)]
     (reduce
      (fn [acc id]
        (assoc acc id (apply-defaults id (fields id))))
      {} (keys fields))))
  ([id {type :type :as field}]
   (merge all-defaults (type-defaults type field) (assoc field :id id))))

(defn mk-config [root title actions & {:as fields}]
  (let [conf {:title title
              :root root
              :fields (apply-defaults fields)}]
    (if actions
      (assoc conf :actions actions)
      conf)))

;:actions
;{:style {:width "20px"}}
