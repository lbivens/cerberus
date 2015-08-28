(ns cerberus.users.view
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
   [om-bootstrap.modal :as md]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [cerberus.utils :refer [goto row display val-by-id]]
   [cerberus.http :as http]
   [cerberus.metadata :as metadata]
   [cerberus.permissions :as permissions]
   [cerberus.view :as view]
   [cerberus.users.api :as users :refer [root]]
   [cerberus.roles.api :as roles]
   [cerberus.orgs.api :as orgs]
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
                      %
                      (val-by-id  "changepass2")
                      :password-validate
                      :password1-val
                      owner)})

      (i/input
       {:type "password" :label "Confirm"
        :id "changepass2"
        :value (:password2-val state)
        :bs-style (if (or (:password-validate state)
                          (blank? (:password2-val state)))
                    nil "error")
        :on-change  #(validate/match
                      %
                      (val-by-id  "changepass1")
                      :password-validate
                      :password2-val
                      owner)})
      (b/button
       {:bs-style "primary"
        :className "pull-right"
        :onClick #(do
                    (alert/raise :success "Password changed")
                    (users/changepass uuid (:password1-val state)))
        :disabled? (false? (:password-validate state))}
       "Change")))))

(defn clean-key [owner]
  (om/set-state! owner :key-name-value "")
  (om/set-state! owner :key-data-value "")
  (om/set-state! owner :add-ssh-modal false)
  (om/set-state! owner :ssh-key-name-edited false))

(defn submit-key [uuid owner state]
  (users/addkey uuid (:key-name-value state) (:key-data-value state))
  (clean-key owner))

(defn add-ssh-modal [uuid owner state]
  (d/div
   {:style {:display (if (:add-ssh-modal state) "block" "none")} }
   (md/modal
    {:header (d/h4 "New SSH Public Key"
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
                         :disabled? (false?
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
       :on-change
       #(do
          (if (not (:ssh-key-name-edited state))
            (om/set-state!
             owner :key-name-value
             (last (clojure.string/split (val-by-id "newsshkey") #" "))))
          (validate/nonempty
           %
           :key-data-validate
           :key-data-value
           owner))})
     (i/input
      {:type "text" :label "Name"
       :id "newsshkeyname"
       :value (:key-name-value state)
       :on-change
       #(do
          (om/set-state! owner :ssh-key-name-edited true)
          (validate/nonempty
           %
           :key-name-validate
           :key-name-value
           owner))})))))

(defn ssh-key-li [uuid key-name key-data]
  (d/li
   {:class "list-group-item"}
   (d/a {onClick #(println "clicked")}
        key-name)
   (b/button {:bs-size "xsmall"
              :className "pull-right"
              :onClick #(users/deletekey uuid key-name)}
             (r/glyphicon {:glyph "remove"}))))


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
               :list-group(d/ul {:class "list-group"}
                                (map (fn [[key-name key-data]]
                                       [(ssh-key-li
                                         uuid
                                         (clojure.string/replace (str key-name) #"^:" "")
                                         key-data)])
                                     ssh-keys ))})
     (add-ssh-modal uuid element state))))

(defn mfa-panel []
  (p/panel {:header (d/h3 "Yubi Keys")}
           "stub"))

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
         (mfa-panel)))))))


(defn render-roles [app owner {:keys [root id]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [element (get-in app [root :elements id])
            roles (get-in app [:roles :elements])
            current-roles (sort (or (:roles element) []))]
        (r/well
         {}
         (row
          (col
           {:xs 10 :sm 4}
           (i/input
            {:type "select" :id "role"}
            (map (fn [[uuid e]] (d/option {:value uuid} (:name e))) roles)))
          (col
           {:xs 2 :sm 1}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :onClick #(users/add-role  id (val-by-id "role"))
             :disabled? (false? (:password-validate state))}
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
                          (r/glyphicon {:glyph "remove"}))))
             current-roles)))))))))

(defn render-orgs [app owner {:keys [root id]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [element (get-in app [root :elements id])
            orgs (get-in app [:orgs :elements])
            current-orgs (sort (or (:orgs element) []))]
        (r/well
         {}
         (row
          (col
           {:xs 10 :sm 4}
           (i/input
            {:type "select" :id "org"}
            (map (fn [[uuid e]] (d/option {:value uuid} (:name e))) orgs)))
          (col
           {:xs 2 :sm 1}
           (b/button
            {:bs-style "primary"
             :className "pull-right"
             :onClick #(users/add-org  id (val-by-id "org"))
             :disabled? (false? (:password-validate state))}
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
                           :onClick #(users/remove-org id uuid)}
                          (r/glyphicon {:glyph "remove"}))
                (if (not= uuid (:org element))
                  (b/button {:bs-size "xsmall"
                             :className "pull-right"
                             :onClick #(users/active-org id uuid)}
                            (r/glyphicon {:glyph "check"})))))
             current-orgs)))))))))

(def sections
  {""         {:key  1 :fn #(om/build render-auth %2)  :title "Authentication"}
   "perms"    {:key  2
               :fn #(om/build permissions/render (get-in %1 [root :elements (get-in %1 [root :selected])])
                              {:opts {:grant users/grant :revoke users/revoke}})
               :title "Permissions"}
   "roles"    {:key  3 :fn #(om/build render-roles %1
                                      {:opts {:id (get-in %1 [root :selected])
                                              :root root}})     :title "Roles"}
   "orgs"     {:key  4 :fn #(om/build render-orgs %1
                                      {:opts {:id (get-in %1 [root :selected])
                                              :root root}})    :title "Orgs"}
   "metadata" {:key  5 :fn #(om/build metadata/render (get-in %1 [root :elements (get-in %1 [root :selected])]))  :title "Metadata"}})

(def render
  (view/make
   root sections
   (fn [data uuid]
     (users/get uuid)
     (orgs/list data)
     (roles/list data))
   {:password-validate false
    :add-ssh-modal false
    :key-name-validate false
    :key-data-validate false}))
