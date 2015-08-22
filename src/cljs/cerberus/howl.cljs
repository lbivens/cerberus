(ns cerberus.howl
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [chord.client :refer [ws-ch]]
   [cerberus.http :as http]
   [cerberus.ws :as ws]
   [cerberus.state :refer [app-state set-state! delete-state!]]
   [cerberus.debug :as dbg]
   [cljs.core.async :refer [<! >! put! close!]]))

(def token-path "sessions/one_time_token")

(def channel (atom))

(defn send [msg]
  (if-let [c @channel]
    (go
      (>! c msg))))

(defn join [channel]
  (if (not (empty? channel))
    (send {:join channel})))

(defn leave [channel]
  (send {:leave channel}))

(defn ping []
  (send {:ping "ping"}))

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
     :data {:action "completed", :uuid snap-id}}]
   (set-state! [:vms :elements channel :snapshots (keyword snap-id) :state] "completed")

   [{:event "snapshot"
     :data  {:action "deleted" :uuid snap-id}}]
   (delete-state! [:vms :elements channel :snapshots (keyword snap-id)])

   [_]
   (dbg/warning "[howl] unknown message:" channel message)))

(defn ws-loop [ws-channel]
  (swap! channel (constantly ws-channel))
  (js/setInterval ping 10000)
  (go
    (loop [c ws-channel]
      (let [{:keys [message]} (<! ws-channel)]
        (match
         [message]
         [{:channel chan :message msg}] (handle-channel chan msg)
         [{:pong _}] :ok
         [{:ok _}] :ok
         [_] (dbg/warning  "[howl] unknown event: " message)))
      (recur c))))

(defn init []
  (go
    (let [response (<! (http/get token-path))]
      (if (= 200 (:status response))
        (let [token (get-in response [:body :token])]
          (go
            (let [{:keys [ws-channel error]} (<! (ws-ch (str (ws/host) "/howl?fifo_ott=" token) {:format :json-kw}))]
              (if-not error
                (ws-loop ws-channel)
                ;(ws-authenticate ws-channel)
                (dbg/error "[howl] ws error:" error)))))
        (dbg/error "[ott] error: " response)))))

