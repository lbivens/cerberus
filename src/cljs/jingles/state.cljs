(ns jingles.state)

(def app-state (atom {:view :root :list []}))
(def app-alerts (atom {}))
(def breadcrumbs (atom []))
(def token (atom))

(defn set-alerts! [alerts]
  (swap! app-alerts assoc :list alerts))

(defn set-alert-type! [type]
  (swap! app-alerts assoc :type type))

(defn set-state! [k v]
  (if (vector? k)
    (swap! app-state assoc-in k v)
    (swap! app-state assoc k v)))


(defn delete-state! [k]
  (swap! app-state dissoc k))

(defn set-view! [section view]
  (set-state! :section section)
  (set-state! :view view))

(defn update-state! [path update-fn]
  (swap! app-state update-in path update-fn))

(defn clear-state! []
  (reset! app-state {}))

(defn print-state []
  (pr @app-state))
