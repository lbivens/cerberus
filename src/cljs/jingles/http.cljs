(ns jingles.http
  (:refer-clojure :exclude [get])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client]
            [cljs-http.core]
            [jingles.utils :refer [path-vec]]
            [jingles.state :refer [app-state set-state!]]))

(enable-console-print!)mk

(defn mk-url [url]
  (clojure.string/join "/" (map #(if (keyword? %) (name %) (str %)) url)))

(defn api [url]
  (str "/api/0.2.0/" (mk-url (path-vec url))))

(defn default-headers []
  {"Accept" "application/json"})

(defn add-headers [req hdrs]
  (assoc req :headers hdrs :oauth-token (:token @app-state)))

(defn do-req [req-fn url hdrs req]
  (let [hdrs (merge hdrs (default-headers))]
    (req-fn (api url) (add-headers req hdrs))))

(defn get
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [hdrs req]]
  (do-req cljs-http.client/get url hdrs req))

(defn post
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [hdrs req]]
  (do-req cljs-http.client/post url hdrs req))

(defn put
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [hdrs req]]
  (do-req cljs-http.client/put url hdrs req))

(defn delete
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [hdrs req]]
  (do-req cljs-http.client/delete url hdrs req))

