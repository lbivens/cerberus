(ns jingles.users.view
  (:require
   [clojure.string :refer [blank?]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.table :refer [table]]
   [om-bootstrap.panel :as p]
   [om-bootstrap.grid :as g]
   [om-bootstrap.button :as b]
   [om-bootstrap.random :as r]
   [om-bootstrap.nav :as n]
   [om-bootstrap.input :as i]
   [jingles.utils :refer [goto grid-row display val-by-id]]
   [jingles.http :as http]
   [jingles.users.api :as users]
   [jingles.users.api :refer [root]]
   [jingles.state :refer [set-state!]]
   [jingles.fields :refer [fmt-bytes fmt-percent]]))

(enable-console-print!)

(defn match-validate [event match validation-key value-key owner]
    (let [newValue (val-by-id  (.. event -target -id))]
      (if (and (= newValue match)
               (not (blank? match))
               (not (blank? newValue))) 
        (om/set-state! owner validation-key true)
        (om/set-state! owner validation-key false))
      (om/set-state! owner value-key newValue)))


(defn password-panel [uuid owner state]
  (p/panel
    {:header (d/h3 "Change Password")}  
    (d/form
            
      (i/input {:type "password" :label "New Password"
                :id "changepass1"
                :value (:password1-val state)
                :on-change  #(match-validate  
                                   %
                                   (val-by-id  "changepass2") 
                                   :password-validate
                                   :password1-val
                                    owner)})
      
      (i/input {:type "password" :label "Confirm"
                :id "changepass2"
                :value (:password2-val state)
                :bs-style (if (or (:password-validate state) 
                                  (blank? (:password2-val state)))
                             nil "error")
                :on-change  #(match-validate 
                                   %
                                   (val-by-id  "changepass1") 
                                   :password-validate
                                   :password2-val
                                    owner)})

       (b/button {:bs-style "primary" 
                          :className "pull-right"
                          :onClick #(users/changepass uuid (:password1-val state))
                          :disabled? (false? (:password-validate state))} 
                          "Change"))))

(defn ssh-keys-panel []
  (p/panel {:header (d/h3 "SSH Keys")}
           "stub"))

(defn mfa-panel []
  (p/panel {:header (d/h3 "Yubi Keys")}
           "stub"))


(defn render-password [data owner state] 
  (let [uuid (get-in data [root :selected])
        element (get-in data [root :elements uuid])]
  (r/well 
    {}
    (grid-row 
      (g/col
           {:md 4}
           (password-panel uuid owner state))
      (g/col
           {:md 4}
           (ssh-keys-panel))
      (g/col
           {:md 4}
           (mfa-panel))
      ))))

(defn render-perms [app owner state] 
  "stub"  
)
(defn render-roles [app owner state] 
  "stub"  
)

(defn render-orgs [app owner state] 
  "stub"  
)


(defn render-metadata [app owner state] 
  "stub"  
)

(def sections {""          {:key  1 :fn render-password  :title "Authentication"}
               "perms"     {:key  2 :fn render-perms     :title "Permissions"}
               "roles"     {:key  3 :fn render-roles     :title "Roles"}
               "orgs"      {:key  4 :fn render-orgs      :title "Orgs"}
               "metadata"  {:key  6 :fn render-metadata  :title "Metadata"}})

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "userdetailview")
    om/IInitState
    (init-state [_]
      {:password-validate false})
    om/IRenderState
    (render-state [_ state]
      (let [uuid (get-in data [root :selected])
            element (get-in data [root :elements uuid])
            section (get-in data [root :section])
            ;key (get-in sections [section :key] 1)
            ]
        (d/div
         {}
         (d/h1 (:name element) " ")
         (d/h6 uuid)         
         (apply n/nav {:bs-style "tabs" :active-key key}
                (map
                 (fn [[section data]]
                   (n/nav-item {:key (:key data)
                                :href (str "#/users/" uuid (if (empty? section) "" (str "/" section)))}
                               (:title data)))
                 (sort-by (fn [[section data]] (:key data)) (seq sections))))
         (if-let [f (get-in sections [section :fn] )]
           (f data owner state)
           (goto (str "#/users/" uuid))))))))