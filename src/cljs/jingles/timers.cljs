(ns jingles.timers
(:require-macros
 [cljs.core.async.macros :refer [go]])
(:require
 [jingles.http :as http]
 [jingles.state :refer [app-state set-state!]]
 [jingles.config]))

(defn cloud-status []
  (if (:token @app-state)
    (go
      (let [resp (<! (http/get "cloud"))
            body (:body resp)]
        (if (:success resp)
          (set-state! :cloud body))))))

(defn tick-10s []
  (jingles.config/flush!)
  (cloud-status))

(tick-10s)

(js/setInterval tick-10s 10000)
