(ns cerberus.timers
(:require-macros
 [cljs.core.async.macros :refer [go]])
(:require
 [cerberus.http :as http]
 [cerberus.api :as api]
 [cerberus.state :refer [app-state set-state!]]
 [cerberus.config :as config]))

(defn cloud-status []
  (if (:token @app-state)
    (go
      (let [resp (<! (http/get "cloud"))
            body (:body resp)]
        (if (:success resp)
          (set-state! :cloud body)
          (if (= 401 (:status resp))
            (do
              (config/logout)
              (api/check-login))))))))

(defn tick-10s []
  (cerberus.config/flush!)
  (cloud-status))

(tick-10s)

(js/setInterval tick-10s 10000)
