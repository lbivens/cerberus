(ns jingles.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [jingles.state :refer [app-state set-state!]]))


(defn to-state [state-path req]
  (go (let [resp (<! req)]
        (set-state! state-path (js->clj (:body resp))))))

(defn full-list [path fields]
  (http/get path {"x-full-list" "true" "x-full-list-fields" fields}))

(defn list [root list-fields]
  (to-state [root :list] (full-list (name root) list-fields)))

(defn get [root uuid]
  (to-state [root :element] (http/get (str (name root) "/" uuid))))



