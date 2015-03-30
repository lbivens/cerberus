(ns jingles.config
  (:refer-clojure :exclude [get print])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [jingles.api :as api]
            [goog.net.cookies]
            [jingles.utils :refer [goto path-vec]]
            [jingles.state :refer [clear-state! app-state set-state! delete-state! update-state!]]))

(enable-console-print!)

(def updates (atom []))

(def metadata-root [:metadata :jingles])

(defn apply-updates [updates]
  (doall
   (map
    (fn [[[section uuid path] value]]
      (api/update-metadata section uuid path value))
    updates))
  [])

(defn add-update [updates path value]
  (conj (vec (filter #(not= (first %) path) updates)) [path value]))

(defn flush! []
  (swap! updates apply-updates))

(defn load []
  (go (let [resp (<! (http/get "sessions"))]
        (if (= 200 (:status resp))
          (let [conf (get-in (:body resp) metadata-root)
                uuid (:uuid (:body resp))]
            (set-state! :config conf)
            (set-state! :user uuid)
            conf)))))

(defn login [token expires-in]
  (do
    (.set goog.net.cookies "token" token expires-in)
    (set-state! :token token)
    (load)
    (goto)))

(defn logout []
  (flush!)
  (.remove goog.net.cookies "token")
  (clear-state!))

(defn clear []
  (go
    (let [_ (pr (:user @app-state))
          req (<! (http/delete (str "users/" (:user @app-state) "/metadata/jingles")))]
      (logout))))

(defn write! [path value]
  (let [path (path-vec path)]
    (if-let [uuid (:user @app-state)]
      (swap! updates add-update [:users uuid (concat [:jingles] path)] value))
    (set-state! (vec (concat [:config] path)) value)
    value))

(defn get
  ([path default]
   (let [path (path-vec path)
         v (get-in @app-state (concat [:config] path) :no-value-set)]
       (if (= v :no-value-set)
         (do
           (write! path default)
           default)
         v)))
  ([path]
   (let [path (path-vec path)]
     (get-in @app-state (concat [:config] path)))))

(defn update! [path update-fn]
  (write! path (update-fn (get path))))

(defn delete! [path]
  (let [uuid (:user @app-state)
        full-path (path-vec path)
        key (last full-path)
        path (butlast full-path)]
    (api/delete-metadata :users uuid (concat [:jingles] full-path))
    (update-state! (concat [:config] path) dissoc key)))

(defn print [] (pr (get-in @app-state [:config])))

(defn user [] (get-in @app-state [:user]))

(if-let [token (.get goog.net.cookies "token")]
  (do
    (set-state! :token token)
    (load)))

(js/setInterval flush! 10000)
