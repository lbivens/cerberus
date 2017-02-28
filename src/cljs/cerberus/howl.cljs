(ns cerberus.howl
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [chord.client :refer [ws-ch]]
   [cerberus.http :as http]
   [cerberus.global :as global]
   [cerberus.state :refer [app-state set-state! delete-state!]]
   [cerberus.debug :as dbg]
   [cljs.core.async :refer [<! >! put! close!]]))

(defn host []
  (let [location (.-location js/window)
        proto (.-protocol js/location)
        ws (clojure.string/replace proto #"^http" "ws")
        host (.-hostname location)
        port (.-port location)]
    (global/get "ws" (str ws "//" host ":" port))))

(def token-path "sessions/one_time_token")

(def channel (atom))

(def joined (atom #{}))

(defn handle-channel [channel message]
  (match
   [message]

   [{:event "state" :data state}]
   (set-state! [:vms :elements channel :state] state)

   [{:event "delete"}]
   (delete-state! [:vms :elements channel])

   [{:event "progress", :data {:imported imported}}]
   (set-state! [:datasets :elements channel :imported] imported)

   [{:event "backup",
     :data  {:action "update", :data data :uuid backup-id}}]
   (let [[[k v]] (vec data)]
     (set-state! [:vms :elements channel :backups (keyword backup-id) k] v))

   [{:event "backup",
     :data  {:action "deleted" :uuid backup-id}}]
   (delete-state! [:vms :elements channel :backups (keyword backup-id)])

   [{:event "snapshot"
     :data {:action "completed", :data data :uuid snap-id}}]
   (set-state! [:vms :elements channel :snapshots (keyword snap-id)] data)


   [{:event "snapshot"
     :data  {:action "deleted" :uuid snap-id}}]
   (delete-state! [:vms :elements channel :snapshots (keyword snap-id)])

   ;; currently only update some fields
   [{:event "update", :data {:config config}}]
   ((set-state! [:vms :elements channel :alias] {:alias config}) ;TODO This cant possible be right
    (set-state! [:vms :elements channel :config :maintain_resolvers] (:maintain_resolvers config)))
   ;; update the owner
   [{:event "update", :data {:owner owner}}]
   ((set-state! [:vms :elements channel :owner] owner))

   [_]
   (dbg/warning "[howl] unknown message:" channel message)))


(defn re-init [init]
  (do
    (if-let [c @channel]
      (do
        (swap! channel (constantly false))
        (close! @channel)))
    (init)))

(defn ping [init]
  (go
    (if (not (>! @channel {:ping "ping"}))
      (re-init init))))

(defn ws-loop [init ws-channel]
  (swap! channel (constantly ws-channel))
  (js/clearInterval (.-howl js/window))
  (set! (.-howl js/window) (js/setInterval #(ping init) 10000))
  (let [c @channel]
    (go
      (loop [channels @joined]
        (>! c {:join (first channels)})
        (if (not (empty? (rest channels)))
          (recur (rest channels))))))
  (go
    (loop []
      (if-let [m (<! @channel)]
        (let [{:keys [message]} m]
          (match
           [message]
           [{:channel chan :message msg}]  (handle-channel chan msg)
           [{:pong _}] :ok
           [{:ok _}] :ok
           [_] (dbg/warning  "[howl] unknown event: " message))
          (recur))
        (re-init init)))))

(defn init []
  (go
    (let [response (<! (http/get token-path))]
      (if (= 200 (:status response))
        (let [token (get-in response [:body :token])]
          (let [{:keys [ws-channel error]} (<! (ws-ch (str (host) "/howl?fifo_ott=" token) {:format :json-kw}))]
            (if-not error
              (ws-loop init ws-channel)
              ;;(ws-authenticate ws-channel)
              (dbg/error "[howl] ws error:" error)))
          (do))
        (dbg/error "[ott] error: " response)))))


(defn send [msg]
  (if-let [c @channel]
    (go
      (>! c msg))))


(defn join-all [channels-in]
  (go
    (loop [uuid (first channels-in)
           channels (rest channels-in)]
      (if uuid
        (do
          (if (not (@joined uuid))
            (do
              (swap! joined conj uuid)
              (if-let [c @channel]
                (>! c {:join uuid}))))
          (recur (first channels) (rest channels)))))))

(defn join [channel]
  (if (not (@joined channel))
    (do
      (swap! joined conj channel)
      (send {:join channel}))))

(defn leave [channel]
  (swap! joined disj channel)
  (send {:leave channel}))
