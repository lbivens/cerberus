(ns jingles.ipranges.create
  (:require
   ;[jingles.ipranges.api :refer [root]]
   [jingles.utils :refer [ip->int]]
   [jingles.create :as create]
   [jingles.config :as conf]))

(defn network []
  (or (conf/get-config [:add :data :network]) "0.0.0.0"))

(defn netmask []
  (or (conf/get-config [:add :data :netmask]) "255.255.255.255"))

(defn gateway []
  (or (conf/get-config [:add :data :gateway]) "0.0.0.0"))

(defn first-ip []
  (or (conf/get-config [:add :data :first]) "0.0.0.0"))

(defn last-ip []
  (or (conf/get-config [:add :data :last]) "0.0.0.0"))

(def ip-re #"^([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])$")


(defn valid-network []
  (let [network (ip->int (network))
        netmask (ip->int (netmask))
        net-last (+ network (bit-not netmask))
        gateway (ip->int (gateway))
        first (ip->int (first-ip))
        last (ip->int (last-ip))]
    (and
     (< network first last net-last)
     (< network gateway net-last)
     (= network (bit-and network netmask)))))

(defn valid-ip [ip]
  (and (re-matches ip-re ip)
       (valid-network)))

(defn render [app]
  (create/render
   app
   {:type :input :label "Name" :id "ipr-name" :key :name}
   {:type :input :label "NIC Tag" :id "ipr-tag" :key :tag}
   {:type :input :label "VLAN" :id "ipr-vlan" :key :vlan :data-type :integer :validator #(<= 0 % 4096)}

   {:type :input :label "Subnet IP" :id "ipr-network" :key :network :validator valid-ip}
   {:type :input :label "Netmask" :id "ipr-netmask" :key :netmask :validator valid-ip}
   {:type :input :label "Gateway" :id "ipr-gateway" :key :gateway :validator valid-ip}
   {:type :input :label "First" :id "ipr-first" :key :first :validator valid-ip}
   {:type :input :label "Last" :id "ipr-last" :key :last :validator valid-ip}))
