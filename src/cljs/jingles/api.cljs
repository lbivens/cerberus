(ns jingles.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [jingles.state :refer [app-state set-state!]]))


(defn to-state
  ([state-path req]
     (go (let [resp (<! req)]
           (set-state! state-path (js->clj (:body resp))))))
  ([state-path req map-fn]
     (go (let [resp (<! req)]
           (set-state! state-path (map map-fn (js->clj (:body resp))))))))

(defn full-list [path fields]
  (http/get path {"x-full-list" "true" "x-full-list-fields" fields}))

(defn list [root list-fields]
  (go
    (let [resp (<! (full-list (name root) list-fields))
          elements (js->clj (:body resp))
          list (map :uuid elements)
          elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
      (set-state! [root :elements] elements)
      (set-state! [root :list] list))))

(defn get [root uuid]
  (to-state [root :elements uuid] (http/get (str (name root) "/" uuid))))

(defn get-sub-element [root key path url element]
  (let [uuid (element key)]
    (if (empty? uuid)
      ""
      (if-let [sub (get-in @app-state [root :relations uuid])]
        (get-in sub path)
        (do
          (to-state [root :relations uuid] (http/get (str url "/" uuid)))
          uuid)))))
