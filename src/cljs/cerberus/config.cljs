(ns cerberus.config
  (:refer-clojure :exclude [get set! print])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.users.api :as user]
   [goog.net.cookies]
   [cerberus.howl :as howl]
   [cerberus.debug :as dbg]
   [cerberus.utils :refer [goto path-vec]]
   [cerberus.state :refer [clear-state! app-state set-state! delete-state! update-state!]]))

(enable-console-print!)


(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(def updates (atom []))

(def metadata-root [:metadata :cerberus])

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
            (swap! app-state #(deep-merge % conf))
            (pr "config: " conf)
            (set-state! :config conf)
            (set-state! :user uuid)
            conf)))))
(defn set! [ks v]
  (user/set-metadata (:user @app-state) (concat  [:cerberus] ks) v))

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
    (let [req (<! (http/delete (str "users/" (:user @app-state) "/metadata/cerberus")))]
      (logout))))

(defn write! [path value]
  (let [path (path-vec path)]
    (if-let [uuid (:user @app-state)]
      (swap! updates add-update [:users uuid (concat [:cerberus] path)] value))
    (set-state! (vec (concat [:config] path)) value)
    value))

(defn global
  ([key dflt]
   (get-in (js->clj js/Config) [key] dflt))
  ([key]
   (get-in (js->clj js/Config) [key]))
  ([]
   (js->clj js/Config)))

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
    (api/delete-metadata :users uuid (concat [:cerberus] full-path))
    (update-state! (concat [:config] path) dissoc key)))

(defn extract-fields [fields]
  (into
   {}
   (map
    (fn [[field opts]]
      [field (select-keys opts [:order :show])])
    fields)))

(defn extract []
  (into
   {}
   (map
    (fn [[name {fields :fields}]]
      [name (extract-fields fields)])
    (filter
     #(not (nil? (:fields (second %)))) @app-state))))

(defn print []
  (dbg/debug "[config] " (extract)))

(defn user [] (get-in @app-state [:user]))

(if-let [token (.get goog.net.cookies "token")]
  (do
    (set-state! :token token)
    (load)
    (howl/init)))
