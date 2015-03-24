(ns jingles.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jingles.http :as http]
            [clojure.string :refer [join]]
            [jingles.utils :refer [value-by-key]]
            [jingles.state :refer [app-state set-state!]]))


(defn to-state
  ([state-path req]
     (go (let [resp (<! req)]
           (set-state! state-path (js->clj (:body resp))))))
  ([state-path req map-fn]
     (go (let [resp (<! req)]
           (set-state! state-path (map map-fn (js->clj (:body resp))))))))

(defn full-list [path fields]
  (http/get path {"x-full-list" "true" "x-full-list-fields" fields}))

(defn list [root list-fields]
  (go
    (let [resp (<! (full-list (name root) list-fields))
          elements (js->clj (:body resp))
          list (map :uuid elements)
          elements (reduce (fn [acc e] (assoc acc (:uuid e) e)) {} elements)]
      (set-state! [root :elements] elements)
      (set-state! [root :list] list))))

(defn get [root uuid]
  (to-state [root :elements uuid] (http/get (str (name root) "/" uuid))))

(defn get-sub-element [root key path element]
  (let [uuid (element key)]
    (if (empty? uuid)
      ""
      (if-let [sub (get-in @app-state [root :elements uuid])]
        (value-by-key path sub)
        (do
          (to-state [root :elements uuid] (http/get (str (name root) "/" uuid)))
          uuid)))))


(defn update-metadata
  ([root uuid path value]
     (let [key (last path)
           path-str (map #(if (keyword? %) (name %) %) (butlast path))
           path-url (str (name  root) "/" uuid "/metadata/"(join "/" path-str))]
       (update-metadata root uuid path value
                        #(go (let [resp  (<! (http/put path-url {} {:json-params (hash-map key value)}))]
                               (if (= 204 (:status resp))
                                 (get root uuid)))))))
  ([root uuid path value done-fn]
     (let [key (last path)
           path-str (map #(if (keyword? %) (name %) %) (butlast path))
           path-url (str (name  root) "/" uuid "/metadata/"(join "/" path-str))]
       (done-fn))))
