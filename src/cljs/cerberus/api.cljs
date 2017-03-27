(ns cerberus.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljsjs.oboe :as oboe]
   [om.core :as om :include-macros true]
   [clojure.set :refer [difference]]
   [cerberus.http :as http]
   [cerberus.howl :as howl]
   [clojure.string :refer [join]]
   [cerberus.alert :refer [raise]]
   [cerberus.utils :refer [goto value-by-key]]
   [cerberus.state :refer [app-state path-vec set-state! delete-state!]]))

(defn check-login []
  (set-state! :token false)
  (goto))

(defn to-state [error-dest state-path req]
  (go (let [resp (<! req)]
        (condp = (:status resp)
          401 (check-login)
          404 (do
                (raise :warning (str (name  (first state-path)) " " (nth state-path 2) " not found!"))
                (pr error-dest)
                (if error-dest (goto error-dest))
                (delete-state! state-path))
          200 (set-state! state-path (js->clj (:body resp)))))))

(defn full-list
  ([path]
   (http/get (str path "?full-list=true")))
  ([path fields]
   (http/get (str path "?full-list=true&full-list-fields=" fields))))

(defn list-path
  [path fields] (if fields
                  (str path "?full-list=true&full-list-fields=" fields)
                  (str path "?full-list=true")))

(defn list
  ([data root]
   (list data root nil))
  ([data root list-fields]
   (->
    (.oboe js/window
           #js{"url" (http/api (list-path (name root) list-fields))
               "headers" #js{"Accept" "application/json"
                             "Authorization" (str "Bearer " (:token @app-state))}})
    (.node
     "!.*",
     (fn [x]
       (let [e (js->clj x :keywordize-keys true)
             uuid (:uuid e)]
         (om/transact! data [root :elements uuid]
                       (fn [old]
                         (merge old e)))
         uuid)))
    (.done
     (fn [all]
       (let [new-keys (set (js->clj all))]
         (howl/join-all new-keys)
         (om/transact! data [root :elements]
                       (fn [old]
                         (let [old-keys (set (keys old))
                               deleted (difference old-keys new-keys)]
                           (reduce dissoc old deleted))))))))))


(defn get [root uuid]
  (to-state false [root :elements uuid] (http/get [root uuid])))


(defn get-page [root uuid]
  (to-state (str "/" (name root)) [root :elements uuid] (http/get [root uuid])))

(defn join-and [other-fn]
  (fn join-and* [resp]
    (if-let [uuid (:uuid (:body resp))]
      (howl/join uuid))
    (other-fn resp)))

(defn post-success-fn [root]
  (fn post-success-fn* [resp]
    (let [body (:body resp)
          uuid (:uuid body)]
      (set-state! [root :elements uuid] body))))

(defn post
  ([root path data]
   (post root path data {:success (post-success-fn root)}))
  ([root path data {:as callbacks :or {:success (post-success-fn root)}}]
   (go
     (let [resp (<! (http/post (concat [root] path) {} {:json-params data}))
           success (join-and (or (:success callbacks) identity))
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
                                         (set-state! [root :elements uuid] body)))}}]
  (go
    (let [resp (<! (http/put (concat [root] path) {} {:json-params data}))
          success (join-and  (or (:success callbacks) identity))
          error (or (:error callbacks) identity)
          status (:status resp)]
      (if-let [always (:always callbacks)]
        (always resp))
      (if-let [callback (callbacks status)]
        (callback resp)
        (if (:success resp)
          (success resp)
          (error resp))))))

(defn delete-path [data root path]
  (if (= 1 (count path))
    (let [uuid (first path)]
      (om/transact! data [root :elements]
                    #(dissoc % uuid)))))

(defn delete
  ([data root path opts]
   (let [success (or (:success opts) identity)]
     (delete root path
             (assoc opts
                    :success #(do
                                (delete-path data root path)
                                success)))))
  ([root path {:as callbacks}]
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
           (error resp)))))))

(defn get-sub-element [root key path element]
  (let [uuid (key element)]
    (if (empty? uuid)
      ""
      (if-let [sub (get-in @app-state [root :elements uuid])]
        (value-by-key path sub)
        (do
          ;;(to-state [root :elements uuid] (http/get [root uuid]))
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
