(ns cerberus.state
  (:require
   [cerberus.debug :as dbg]))

(def app-state (atom {:valid-login true :view :root :list []}))
(def app-alerts (atom {}))
(def breadcrumbs (atom []))
(def token (atom))

;; For some reason vectors are note seq's
(defn vec-or-seq? [e]
  (or (vector? e) (seq? e)))

(defn path-vec [e]
  (if (vec-or-seq? e) e [e]))

(defn set-alerts! [alerts]
  (swap! app-alerts assoc :list alerts))

(defn set-alert-type! [type]
  (swap! app-alerts assoc :type type))

(defn set-state! [k v]
  (if (vector? k)
    (swap! app-state assoc-in k v)
    (swap! app-state assoc k v)))

(defn update-state! [path update-fn & args]
  (let [path (if (vec-or-seq? path) path [path])]
    (swap! app-state update-in path #(apply update-fn % args))))

(defn delete-state! [path]
  (let [path (path-vec path)
        key (last path)
        path (butlast path)]
    (if (= (count path) 0)
      (swap! app-state dissoc key)
      (update-state! path dissoc key))))

(defn set-view! [section view]
  (set-state! :section section)
  (set-state! :view view)
  (set-state! [:add :view-section] section))

(defn clear-state! []
  (delete-state! :token)
  (doall
   (map #(if (get-in @app-state [% :elements])
           (delete-state! [% :elements])) (keys @app-state))))

(defn print-state []
  (pr @app-state))
