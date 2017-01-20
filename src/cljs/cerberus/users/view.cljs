(ns cerberus.users.view
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :as cstr]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g :refer [col]]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [om-bootstrap.modal :as md]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [goto row display val-by-id ->state]]
   [cerberus.http :as http]
   [cerberus.metadata :as metadata]
   [cerberus.permissions :as permissions]
   [cerberus.view :as view]
   [cerberus.users.api :as users :refer [root]]
   [cerberus.roles.api :as roles]
   [cerberus.scopes.api :as scopes]
   [cerberus.orgs.api :as orgs]
   [cerberus.clients.api :as clients]
   [cerberus.alert :as alert]
   [cerberus.state :refer [set-state!]]
   [cerberus.fields :refer [fmt-bytes fmt-percent]]
   [cerberus.validate :as validate]))

(defn password-panel [data owner state]
  (let [uuid (:uuid data)]
    (p/panel
     {:header (d/h3 "Change Password")}
     (d/form
      (i/input
       {:type "password" :label "New Password"
        :id "changepass1"
        :value (:password1-val state)
        :on-change  #(validate/match
                      (val-by-id  "changepass2")
                      :password-validate
                      :password1-val
                      owner %)})
      (i/input
       {:type "password" :label "Confirm"
        :id "changepass2"
        :value (:password2-val state)
        :bs-style (if (or (:password-validate state)
                          (cstr/blank? (:password2-val state)))
                    nil "error")
        :on-change  #(validate/match
                      (val-by-id  "changepass1")
                      :password-validate
                      :password2-val
                      owner %)})
      (b/button
       {:bs-style "primary"
        :className "pull-right"
        :onClick #(do
                    (alert/raise :success "Password changed")
                    (users/changepass uuid (:password1-val state)))
        :disabled? (false? (:password-validate state))}
       "Change")))))

(defn clean-key [owner]
  (om/set-state! owner :key-name-validate false)
  (om/set-state! owner :key-name-value "")
  (om/set-state! owner :key-data-validate false)
  (om/set-state! owner :key-data-value "")
  (om/set-state! owner :add-ssh-modal false)
  (om/set-state! owner :ssh-key-name-edited false))

(defn submit-key [uuid owner state]
  (users/add-sshkey uuid (:key-name-value state) (:key-data-value state))
  (clean-key owner))

(defn change-key [state owner event]
  (if (not (:ssh-key-name-edited state))
    (do (om/set-state!
         owner :key-name-validate true)
        (om/set-state!
         owner :key-name-value
         (last (clojure.string/split (val-by-id "newsshkey") #" ")))))
  (validate/nonempty
   :key-data-validate
   :key-data-value
   owner event))

(defn add-ssh-modal [uuid owner state]
  (d/div
   {:style {:display (if (:add-ssh-modal state) "block" "none")} }
   (md/modal
    {:header (d/h4
              "New SSH Public Key"
              (d/button {:type         "button"
                         :class        "close"
                         :aria-hidden  true
                         :on-click #(clean-key owner)}
                        "×"))
     :close-button? false
     :visible? true
     :animate? false
     :style {:display "block"}
     :footer (d/div
              (b/button {:bs-style "success"
                         :disabled? (not
                                     (and
                                      (:key-name-validate state)
                                      (:key-data-validate state)))
                         :on-click #(submit-key uuid owner state)}
                        "Add"))}
    (d/form
     (i/input
      {:type "textarea" :label "Key"
       :id "newsshkey"
       :style {:height "8em"}
       :value (:key-data-value state)
       :on-change (partial change-key state owner)})
     (i/input
      {:type "text" :label "Name"
       :id "newsshkeyname"
       :value (:key-name-value state)
       :on-change
       #(do
          (om/set-state! owner :ssh-key-name-edited true)
          (validate/nonempty
           :key-name-validate
           :key-name-value
           owner %))})))))

(defn clean-yubi [owner]
  (om/set-state! owner :yubi-value "")
  (om/set-state! owner :add-yubi-modal false))

(defn submit-yubi [uuid owner state]
  (users/add-yubikey uuid (:yubi-value state))
  (clean-yubi owner))

(defn add-yubi-modal [uuid owner state]
  (d/div
   {:style {:display (if (:add-yubi-modal state) "block" "none")} }
   (md/modal
    {:header (d/h4
              "Register YubiKey"
              (d/button {:type         "button"
                         :class        "close"
                         :aria-hidden  true
                         :on-click #(clean-yubi owner)}
                        "×"))
     :close-button? false
     :visible? true
     :animate? false
     :style {:display "block"}
     :footer (d/div
              (b/button {:bs-style "success"
                         :disabled? (not
                                     (:yubi-validate state))
                         :on-click #(submit-yubi uuid owner state)}
                        "Add"))}
    (d/form
     (i/input
      {:type "text" :label "Key"
       :id "newyubikey"
       :value (:yubi-value state)
       :on-change
       #(validate/nonempty
         :yubi-validate
         :yubi-value
         owner %)})))))

(defn ssh-key-li [uuid key-name key-data]
  (d/li
   {:class "list-group-item"}
   (d/a {onClick #(println "clicked")}
        key-name)
   (b/button {:bs-size "xsmall"
              :className "pull-right"
              :onClick #(users/delete-sshkey uuid key-name)}
             (r/glyphicon {:glyph "trash"}))))

(defn ssh-keys-panel [data element state]
  (let [uuid (:uuid data)
        ssh-keys (:keys data)]
    (d/div
     (p/panel {:header (d/h3 "SSH Keys"
                             (b/button {;:bs-style "primary"
                                        :bs-size "xsmall"
                                        :className "pull-right"
                                        :onClick #(om/set-state! element :add-ssh-modal true)}
                                       (r/glyphicon {:glyph "plus"})))
               :list-group (d/ul {:class "list-group"}
                                 (map (fn [[key-name key-data]]
                                        [(ssh-key-li
                                          uuid
                                          (name key-name)
                                          key-data)])
                                      ssh-keys ))})
     (add-ssh-modal uuid element state))))

(defn yubi-key-li [uuid key-name]
  (d/li
   {:class "list-group-item"}
   (d/a {onClick #(println "clicked")}
        key-name)
   (b/button {:bs-size "xsmall"
              :className "pull-right"
              :onClick #(users/delete-yubikey uuid key-name)}
             (r/glyphicon {:glyph "trash"}))))

(defn mfa-panel [data element state]
  (let [uuid (:uuid data)
        yubi-keys (:yubikeys data)]
    (d/div
     (p/panel {:header (d/h3 "Yubi Keys"
                             (b/button {;:bs-style "primary"
                                        :bs-size "xsmall"
                                        :className "pull-right"
                                        :onClick #(om/set-state! element :add-yubi-modal true)}
                                       (r/glyphicon {:glyph "plus"})))
               :list-group(d/ul {:class "list-group"}
                                (map (fn [key-id]
                                       [(yubi-key-li
                                         uuid
                                         key-id)])
                                     yubi-keys ))})
     (add-yubi-modal uuid element state))))

(defn render-auth [data owner opts]
  (reify
    om/IRenderState
    (render-state [_ state]
      (r/well
       {}
       (row
        (col
         {:md 4}
         (password-panel data owner state))
        (col
         {:md 4}
         (ssh-keys-panel data owner state))
        (col
         {:md 4}
         (mfa-panel  data owner state)))))))

(defn render-roles [app owner {:keys [root id]}]
  (reify
    om/IInitState
    (init-state [_]
      {:role (or (first (first (get-in app [:roles :elements]))) "")})
    om/IRenderState
    (render-state [_ state]
      (let [element (get-in app [root :elements id])
            roles (get-in app [:roles :elements])
            current-roles (sort (or (:roles element) []))
            invalid-role (set (cons "" current-roles))]
        (r/well
         {}
         (row
          (col
           {:xs 10 :sm 4}
           (i/input
            {:type "select"
             :value (:role state)
             :on-change (->state owner :role)}
            (map (fn [[uuid e]] (d/option {:value uuid} (:name e)))
                 (sort-by #(:name (second %)) roles))))
          (col
           {:xs 2 :sm 1}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :onClick #(users/add-role id (:role state))
             :disabled? (invalid-role (:role state))}
            "Add"))
          (col
           {:xs 12 :sm 6}
           (d/ul
            (map
             (fn [uuid]
               (d/li
                (d/a {href (str "#/roles/" uuid)} (get-in roles [uuid :name]))
                (b/button {:bs-size "xsmall"
                           :className "pull-right"
                           :onClick #(users/remove-role id uuid)}
                          (r/glyphicon {:glyph "trash"}))))
             current-roles)))))))))

(defn render-orgs [app owner {:keys [id]}]
  (reify
    om/IInitState
    (init-state [_]
      {:org (or (first (first (get-in app [:orgs :elements]))) "")})
    om/IRenderState
    (render-state [_ state]
      (let [element (get-in app [root :elements id])
            orgs (get-in app [:orgs :elements])
            current-orgs (sort (or (:orgs element) []))
            invalid-org (set (cons "" current-orgs))]
        (r/well
         {}
         (row
          (col
           {:xs 10 :sm 4}
           (i/input
            {:type "select"
             :value (:org state)
             :on-change (->state owner :org)}
            (map (fn [[uuid e]] (d/option {:value uuid} (:name e)))
                 (sort-by #(:name (second %)) orgs))))
          (col
           {:xs 2 :sm 1}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :on-click #(users/add-org id (:org state))
             :disabled? (invalid-org (:org state))}
            "Add"))
          (col
           {:xs 12 :sm 6}
           (d/ul
            (map
             (fn [uuid]
               (d/li
                (d/a {href (str "#/orgs/" uuid)} (get-in orgs [uuid :name]))
                (b/button {:bs-size "xsmall"
                           :className "pull-right"
                           :bs-style "danger"
                           :onClick #(users/remove-org id uuid)}
                          (r/glyphicon { :glyph "trash"}))
                (if (not= uuid (:org element))
                  (b/button {:bs-size "xsmall"
                             :bs-style "success"
                             :className "pull-right"
                             :onClick #(users/active-org id uuid)}
                            (r/glyphicon {:glyph "check"}))
                  " (active)")))
             current-orgs)))))))))


(defn token-modal [owner state]
  (d/div
   {:style {:display (if (:token state) "block" "none")} }
   (md/modal
    {:header (d/h4
              "API Key"
              (d/button {:type         "button"
                         :class        "close"
                         :aria-hidden  true
                         :on-click #(do
                                      (om/set-state! owner :comment "")
                                      (om/set-state! owner :token false)
                                      (om/set-state! owner :token false))}
                        "×"))
     :close-button? false
     :visible? true
     :animate? false
     :style {:display "block"}}
    (d/p
     "Your API key as been created, this is the only time you will be able to access it, "
     "be sure to put it in the applicaiton you generated it for." (d/br)
     (d/strong "Once this window is closed you will not be able to retrive it any again!"))
    (d/p
     "API Token: " (d/strong {:class "uuid"} (:token state))))))

(defn create-api-key [owner {:keys [comment scopes uuid] :as state}]
  (go
    (let [data     {:comment (cstr/trim comment) :scope scopes}
          resp     (<! (http/post [root uuid :tokens] {} {:json-params data}))]
      (if (= 200 (:status resp))
        (do
          (users/get uuid)
          (om/set-state! owner :token (:token (js->clj (:body resp)))))))))

(defn render-tokens [app owner {:keys [id]}]
  (reify
    om/IInitState
    (init-state [_]
      {:comment ""
       :token   false
       :uuid    id
       :scopes  (set (map :scope (filter :default (get-in app [scopes/root :elements]))))})
    om/IRenderState
    (render-state [_ state]
      (let [element (get-in app [root :elements id])
            tokens (sort-by :client (:tokens element))
            client-name #(get-in app [clients/root :elements % :name])
            scopes (get-in app [scopes/root :elements])]
        (d/div
         {}
         (token-modal owner state)
         (r/well
          {}

          (g/row
           {}
           (g/col
            {:xs 12 :sm 8 :md 10}
            (i/input
             {:type "text"
              :value (:comment state)
              :placeholder "API Key Name"
              :on-change (->state owner :comment)}))
           (g/col
            {:xs 12 :sm 4 :md 2}
            (b/button {:bs-style "primary"
                       :disabled? (or (empty? (:scopes state))
                                      (empty? (:comment state)))
                       :on-click #(create-api-key owner state)}
                      "Create API Key")))
          (g/row
           {}
           (map
            (fn [{scope :scope desc :description}]
              (let [checked ((:scopes state) scope)]
                (g/col
                 {:xs 12 :sm 6 :md 4 :lg 3}
                 (i/input {:type "checkbox" :label desc
                           :on-click (fn []
                                       (if checked
                                         (om/update-state! owner [:scopes] #(disj % scope))
                                         (om/update-state! owner [:scopes] #(conj % scope))))
                           :checked checked}))))
            scopes))
          (g/row
           {}
           (g/col
            {:xs 12}
            (table
             {}
             (d/thead
              {}
              (d/tr
               (d/th "Client")
               (d/th "Type")
               (d/th "Expiery")
               (d/th "Revoke")))
             (d/tbody
              (map (fn [{type     :type
                         expiery  :expiery
                         comment  :comment
                         token-id :id
                         client   :client}]
                     (d/tr
                      (d/td (cond
                              (and (not expiery) (not client)) (d/strong comment)
                              (and (= type "access") (not client)) (d/strong "User")
                              :else (client-name client)))
                      (d/td (cond
                              (and (not expiery) (not client)) "API"
                              (and (= type "access") (not client)) "password"
                              :else type))
                      (d/td
                       (if expiery (str (js/Date. (* expiery 1000))) "never"))
                      (d/td
                       (b/button {:bs-size "xsmall"
                                  :className "pull-right"
                                  :onClick #(users/revoke-token id token-id)}
                                 (r/glyphicon {:glyph "trash"})))))
                   tokens)))))))))))

(def sections
  {""         {:key  1 :fn #(om/build render-auth %2)  :title "Authentication"}
   "perms"    {:key  2
               :fn #(om/build permissions/render
                              %1
                              {:key  (str  (:uuid %2) "-permissions")
                               :opts {:element %2 :grant users/grant :revoke users/revoke}})
               :title "Permissions"}
   "roles"    {:key  3 :fn #(om/build render-roles %1
                                      {:opts {:id (get-in %1 [root :selected])
                                              :root root}})     :title "Roles"}
   "orgs"     {:key  4 :fn #(om/build render-orgs %1
                                      {:opts {:id (get-in %1 [root :selected])}})
               :title "Orgs"}
   "tokens"   {:key  5 :fn #(om/build render-tokens %1
                                      {:opts {:id (get-in %1 [root :selected])}})
               :title "Tokens"}

   "metadata" {:key  6 :fn #(om/build metadata/render 
         (:metadata (get-in %1 [root :elements (get-in %1 [root :selected])])) {:opts {:root "users" :uuid (:uuid %2)}})  :title "Metadata"}})

(def render
  (view/make
   root sections
   users/get
   :mount-fn (fn [uuid data]
               (orgs/list data)
               (clients/list data)
               (scopes/list data)
               (roles/list data))
   :name-fn :name
   :init-state {:password-validate false
                :add-ssh-modal false
                :key-name-validate false
                :key-data-validate false}))
