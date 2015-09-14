(ns cerberus.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.http :as http]
   [cerberus.howl :as howl]
   [clojure.string :refer [join]]
   [cerberus.utils :refer [goto value-by-key]]
   [cerberus.state :refer [app-state path-vec set-state! delete-state!]]))

(defn check-login []
  (set-state! :token false)
  (goto))

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
           (do
             (doall (map #(howl/join %) (keys elements)))
             (om/transact! data [root :elements] (constantly elements)))))))
  ([data root list-fields]
     (go
       (let [resp (<! (full-list (name root) list-fields))
             elements (map-indexed #(assoc %2 :react-key (* 100 %2)) (js->clj (:body resp)))
             elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
         (if (= 401 (:status resp))
           (check-login)
           (do
             (doall (map howl/join (keys elements)))
             (om/transact! data [root :elements] (constantly elements))))))))

(defn get [root uuid]
  (to-state [root :elements uuid] (http/get (str (name root) "/" uuid))))

(defn post-success-fn [root]
  (fn post-success-fn* [resp]
    (let [body (:body resp)
          uuid (:uuid body)]
      (pr "success got:" body)
      (howl/join uuid)
      (set-state! [root :elements uuid] body))))

(defn post
  ([root path data]
   (post root path data {:success (post-success-fn root)}))
  ([root path data {:as callbacks :or {:success (post-success-fn root)}}]
   (go
     (let [resp (<! (http/post (concat [root] path) {} {:json-params data}))
           success (or (:success callbacks) identity)
           error (or (:error callbacks) identity)
           status (:status resp)]
       (if-let [always (:always callbacks)]
         (always resp))
       (if-let [callback (callbacks status)]
         (callback resp)
         (if (:success resp)
           (success resp)
           (error resp)))))))

(defn put [root path data {:as callbacks :or
                           {:success (fn [resp]
                                       (let [body (:body resp)
                                             uuid (:uuid body)]
                                         (howl/join uuid)
                                         (set-state! [root :elements uuid] body)))}}]
  (go
    (let [resp (<! (http/put (concat [root] path) {} {:json-params data}))
          success (or (:success callbacks) identity)
          error (or (:error callbacks) identity)
          status (:status resp)]
      (if-let [always (:always callbacks)]
        (always resp))
      (if-let [callback (callbacks status)]
        (callback resp)
        (if (:success resp)
          (success resp)
          (error resp))))))

(defn delete [root path {:as callbacks}]
  (go
    (let [resp (<! (http/delete (concat [root] path) {}))
          success (or (:success callbacks) identity)
          error (or (:error callbacks) identity)
          status (:status resp)]
      (if-let [always (:always callbacks)]
        (always resp))
      (if-let [callback (callbacks status)]
        (callback resp)
        (if (:success resp)
          (success resp)
          (error resp))))))

(defn get-sub-element [root key path element]
  (let [uuid (key element)]
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


(defn delete-metadata [root uuid path]
  (request-and-get http/delete root uuid (concat [:metadata] path)))
