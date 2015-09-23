(ns cerberus.scopes.api
  (:refer-clojure :exclude [get list])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cerberus.api :as api]
   [cerberus.global :as global]
   [cerberus.alert :refer [alerts]]
   [cerberus.state :refer [set-state!]]))

(def root :scopes)

(defn list [data]
  (go
    (let [resp     (<! (api/full-list (name root)))
          elements (js->clj (:body resp))]
      (if (= 401 (:status resp))
        (api/check-login)
        (om/update! data [root :elements] elements)))))
