(ns jingles.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [jingles.http :as http]
   [clojure.string :refer [join]]
   [jingles.utils :refer [goto value-by-key]]
   [jingles.state :refer [app-state path-vec set-state! delete-state!]]))

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
  ([data root]
     (go
       (let [resp (<! (full-list (name root)))
             elements (js->clj (:body resp))
             elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
         (if (= 401 (:status resp))
           (check-login)
           (om/transact! data [root :elements] (constantly elements))))))
  ([data root list-fields]
     (go
       (let [resp (<! (full-list (name root) list-fields))
             elements (map-indexed #(assoc %2 :react-key (* 100 %2)) (js->clj (:body resp)))
             elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
         (if (= 401 (:status resp))
           (check-login)
           (om/transact! data [root :elements] (constantly elements)))))))

(defn get [root uuid]
  (to-state [root :elements uuid] (http/get (str (name root) "/" uuid))))

(defn post
  ([root path data callback]
   (go
     (let [resp (<! (http/post (concat [root] path) {} {:json-params data}))]
       (callback resp))))
  ([root path data]
   (post root path data (fn [resp]
                          (if (:success resp)
                            (let [body (:body resp)
                                  uuid (:uuid body)]
                              (set-state! [root :elements uuid] body)))))))

(defn put
  ([root path data]
    (go
      (let [resp (<! (http/put (concat [root] path) {} {:json-params data}))]
        #_(if (:success resp)
          (let [body (:body resp)
                uuid (:uuid body)]
            (set-state! [root :elements uuid] body))))))
  ([root path data callback args]
    (go
      (let [resp (<! (http/put (concat [root] path) {} {:json-params data}))]
        #_(if (:success resp)
          (let [body (:body resp)
                uuid (:uuid body)]
            (set-state! [root :elements uuid] body)))
        (apply callback args)))))

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


(defn delete
  ([root path]
   (go
     (let [resp (<! (http/delete (concat [root] path)))])))
  ([root path callback]
   (go
     (let [resp (<! (http/delete (concat [root] path)))]
       (callback resp)))))

(defn delete-metadata [root uuid path]
  (request-and-get http/delete root uuid (concat [:metadata] path)))
