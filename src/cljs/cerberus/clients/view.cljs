(ns cerberus.clients.view
  (:require
   [clojure.string :refer [blank?]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g :refer [col]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [goto row display val-by-id ->state]]
   [cerberus.http :as http]
   [cerberus.api :as api]
   [cerberus.clients.api :refer [root] :as clients]
   [cerberus.view :as view]
   [cerberus.alert :as alert]
   [cerberus.permissions :as permissions]
   [cerberus.metadata :as metadata]
   [cerberus.state :refer [set-state!]]
   [cerberus.validate :as validate]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]))

(defn secret-panel [data owner state]
  (let [uuid (:uuid data)]
    (p/panel
     {:header (d/h3 "Change Secret")}
     (d/form
      (i/input
       {:type "secret" :label "New Secret"
        :id "changepass1"
        :value (:secret1-val state)
        :on-change  #(validate/match
                      (val-by-id  "changepass2")
                      :secret-validate
                      :secret1-val
                      owner %)})

      (i/input
       {:type "secret" :label "Confirm"
        :id "changepass2"
        :value (:secret2-val state)
        :bs-style (if (or (:secret-validate state)
                          (blank? (:secret2-val state)))
                    nil "error")
        :on-change  #(validate/match
                      (val-by-id  "changepass1")
                      :secret-validate
                      :secret2-val
                      owner %)})
      (b/button
       {:bs-style "primary"
        :className "pull-right"
        :onClick #(clients/change-secret uuid (:secret1-val state))
        :disabled? (false? (:secret-validate state))}
       "Change")))))


(defn render-auth [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ state]
      (r/well
       {}
       (row
        (col
         {:md 4}
         (secret-panel data owner state)))))))


(def sections {""         {:key  1 :fn #(om/build render-auth %2)  :title "Authentication"}
               "permissions" {:key  2 :fn #(om/build permissions/render %2 {:opts {:grant clients/grant :revoke clients/revoke}}) :title "Permissions"}
               "metadata"    {:key  3 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections clients/get :name-fn :name))
