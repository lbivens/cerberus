(ns jingles.config
  (:refer-clojure :exclude [get])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [jingles.api :as api]
            [jingles.state :refer [app-state set-state! delete-state! update-state!]]))

(enable-console-print!)


(def updates (atom []))

(defn add-update [updates path value]
  (conj (vec (filter #(not= (first %) path) updates)) [path value]))

(def metadata-root [:metadata :jingles])

(defn load []
  (go (let [resp (<! (http/get "sessions"))]
        (if (= 200 (:status resp))
          (let [conf (get-in (:body resp) metadata-root)
                uuid (:uuid (:body resp))]
            (set-state! :config conf)
            (set-state! :user uuid)
            conf)))))

(defn write! [path value]
  (do
    (if-let [uuid (:user @app-state)]
      (swap! updates add-update [:users uuid (vec (concat [:jingles] path))] value)
      )
    (set-state! (vec (concat [:config] path)) value)
    value))

(defn get
  ([path default]
     (let [v (get-in @app-state (concat [:config] path):no-value-set)]
       (if (= v :no-value-set)
         (do
           (write! path default)
           default)
         v)))
  ([path]
     (get-in @app-state (concat [:config] path))))

(defn update! [path update-fn]
  (write! path (update-fn (get path))))

(defn delete! [path]
  (if (and (vector? path) (> (count path) 1))
    (let [key (last path)
          path (butlast path)]
      (update! path #(dissoc % key)))
    (update-state! [:config] #(dissoc % path))))


(defn apply-updates [updates]
  (do
    (doall
     (map
      (fn [[[section uuid path] value]]
        (api/update-metadata section uuid path value))
      updates))
    []))

(defn flush! []
  (swap! updates apply-updates))

(js/setInterval flush! 10000)
