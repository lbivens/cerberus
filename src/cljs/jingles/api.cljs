(ns jingles.api
  (:refer-clojure :exclude [get])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [jingles.state :refer [app-state set-state!]]))

(defn full-list [path fields]
  (http/get path {"x-full-list" "true" "x-full-list-fields" fields}))


(defn to-state [state-path req]
  (go (let [resp (<! req)]
        (set-state! state-path (js->clj (:body resp))))))
