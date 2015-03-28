(ns jingles.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [clojure.string :refer [join]]
            [jingles.utils :refer [goto value-by-key]]
            [jingles.state :refer [app-state set-state! delete-state!]]))


(defn check-login []
  (do
    (set-state! :token false)
    (goto)))

(defn to-state
  ([state-path req]
     (go (let [resp (<! req)]
           (set-state! state-path (js->clj (:body resp))))))
  ([state-path req map-fn]
     (go (let [resp (<! req)]
           (if (= 401 (:status resp))
             (check-login)
             (set-state! state-path (map map-fn (js->clj (:body resp)))))))))

(defn full-list
  ([path]
     (http/get path {"x-full-list" "true"}))
  ([path fields]
     (http/get path {"x-full-list" "true" "x-full-list-fields" fields})))

(defn list
  ([root]
     (go
       (let [resp (<! (full-list (name root)))
             elements (js->clj (:body resp))
             elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
         (if (= 401 (:status resp))
           (check-login)
           (set-state! [root :elements] elements)))))
  ([root list-fields]
     (go
       (let [resp (<! (full-list (name root) list-fields))
             elements (js->clj (:body resp))
             elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
         (if (= 401 (:status resp))
           (check-login)
           (set-state! [root :elements] elements))))))

(defn get [root uuid]
  (to-state [root :elements uuid] (http/get (str (name root) "/" uuid))))



(defn post [root data]
  (go
    (let [resp (<! (http/post (name root) {} {:json-params data}))]
      (if (:success resp)
        (let [body (:body resp)
              uuid (:uuid body)]
          (set-state! [root :elements uuid] body))))))

(defn get-sub-element [root key path element]
  (let [uuid (element key)]
    (if (empty? uuid)
      ""
      (if-let [sub (get-in @app-state [root :elements uuid])]
        (value-by-key path sub)
        (do
          (to-state [root :elements uuid] (http/get (str (name root) "/" uuid)))
          uuid)))))

(defn request-and-get
  ([request root uuid path]
     (let [path-str (map #(if (keyword? %) (name %) %) path)
           path-url (str (name  root) "/" uuid "/" (join "/" path-str))]
       (go (let [resp  (<! (request path-url))]
             (if (and (>= (:status resp) 200) (< (:status resp) 300))
               (get root uuid))))))
  ([request root uuid path value]
     (let [path-str (map #(if (keyword? %) (name %) %) path)
           path-url (str (name  root) "/" uuid "/" (join "/" path-str))]
       (go (let [resp  (<! (request path-url {} {:json-params value}))]
             (if (and (>= (:status resp) 200) (< (:status resp) 300))
               (get root uuid)))))))

(defn update-metadata [root uuid path value]
  (let [key (last path)
        path (butlast path)]
    (request-and-get http/put root uuid (concat [:metadata] path) (hash-map key value))))


(defn delete [root uuid]
  (go
    (let [req (<! (http/delete [root uuid]))]
      (if (:success req)
        (delete-state! [root :elements uuid])
        (pr req)
        ))))

(defn delete-metadata [root uuid path]
  (request-and-get http/delete root uuid (concat [:metadata] path)))
