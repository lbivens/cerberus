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


(defn render-uris [element owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "client-uris")
    om/IInitState
    (init-state [_]
      {})
    om/IRenderState
    (render-state [_ state]
      (let [uris   (:redirect_uris element)
            uuid   (:uuid element)
            uri    (:uri state)
            invalid? (empty? uri)]
        (r/well
         {}
         (row
          (g/col
           {:sm 9 :md 10}
           (i/input
            {:type "text"
             :placeholder "URI"
             :value uri
             :on-change (->state owner :uri)}))
          (g/col
           {:sm 3 :md 2}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :on-click #(clients/add-uri uuid (:uri state))
             :disabled? invalid?}
            "Add Redirect URI")))
         (row
          (g/col
           {}
           (table
            {}
            (d/thead
             (d/tr
              (d/th "URI")
              (d/th "")))
            (d/tbody
             (map
              (fn [[uid u]]
                (d/tr
                 (d/td u)
                 (d/td
                  (b/button {:bs-size "xsmall"
                             :className "pull-right"
                             :on-click #(clients/delete-uri uuid (name uid))}
                            (r/glyphicon {:glyph "remove"})))))
              uris))))))))))


(def sections {""            {:key  1 :fn #(om/build render-auth %2)  :title "Authentication"}
               "permissions" {:key  2 :fn #(om/build permissions/render %2 {:opts {:grant clients/grant :revoke clients/revoke}}) :title "Permissions"}
               "uris"        {:key  3 :fn #(om/build render-uris %2) :title "URI's"}
               "metadata"    {:key  4 :fn #(om/build metadata/render %2)  :title "Metadata"}})

(def render (view/make root sections clients/get :name-fn :name))
