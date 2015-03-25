(ns jingles.config
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [jingles.api :as api]
            [jingles.state :refer [app-state set-state! delete-state! update-state!]]))

(enable-console-print!)

(def metadata-root [:metadata :jingles])

(defn load []
  (go (let [resp (<! (http/get "sessions"))]
        (if (= 200 (:status resp))
          (let [conf (get-in (:body resp) metadata-root)
                uuid (:uuid (:body resp))]
            (set-state! :config conf)
            (set-state! :user uuid)
            conf)))))

(defn set-config! [path value]
  (do
    (if-let [uuid (:user @app-state)]
      (api/update-metadata :users uuid (vec (concat [:jingles] path)) value))
    (set-state! (vec (concat [:config] path)) value)
    value))

(defn get-config
  ([path default]
     (let [v (get-in @app-state (concat [:config] path):no-value-set)]
       (if (= v :no-value-set)
         (do
           (set-config! path default)
           default)
         v)))
  ([path]
     (get-in @app-state (concat [:config] path))))

(defn update-config! [path update-fn]
  (set-config! path (update-fn (get-config path))))

(defn delete-config! [path]
  (if (and (vector? path) (> (count path) 1))
    (let [key (last path)
          path (butlast path)]
      (update-config! path #(dissoc % key)))
    (update-state! [:config] #(dissoc % path))))



