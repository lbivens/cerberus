(ns jingles.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [goog.net.cookies :as cks]
            [cljs-http.client :as httpc]
            [cljs.core.match]
            [om-tools.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [om-bootstrap.grid :as g]
            [om-bootstrap.input :as i]
            [om-bootstrap.nav :as n]
            [jingles.routing]
            [jingles.http :as http]

            [jingles.hypervisors :as hypervisors]
            [jingles.datasets :as datasets]

            [jingles.vms :as vms]
            [jingles.vms.create :as vms-create]
            [jingles.packages :as packages]
            [jingles.packages.create :as packages-create]
            [jingles.networks :as networks]
            [jingles.networks.create :as networks-create]
            [jingles.ipranges :as ipranges]
            [jingles.ipranges.create :as ipranges-create]
            [jingles.dtrace :as dtrace]
            [jingles.dtrace.create :as dtrace-create]
            [jingles.users :as users]
            [jingles.users.create :as users-create]
            [jingles.roles :as roles]
            [jingles.roles.create :as roles-create]
            [jingles.orgs :as orgs]
            [jingles.orgs.create :as orgs-create]

            [jingles.config :as conf]

            [jingles.list :as jlist]

            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state set-state!]]))

(enable-console-print!)

(set-state! :text "Hello Chestnut!")

(defn login [app]
  (let [path "/api/0.2.0/oauth/token"
        login (fn []
                ;; we need to use post because cljs-http does not allow empty replies :(
                (go (let [response (<! (httpc/post path {:form-params
                                                         {:grant_type "password"
                                                          :username (val-by-id "login")
                                                          :password (val-by-id "password")}}))]
                      (if (= 200 (:status response))
                        (let [e (js->clj (. js/JSON (parse (:body response))))
                              token (e "access_token")
                              expires-in (e "expires_in")]
                          (.set goog.net.cookies "token" token expires-in)
                          (set-state! :token token)
                          (conf/load)
                          (goto))))))]
    (r/well
     {:style {:max-width 400
              :margin "300px auto 10px"}}
     (d/form
      nil
      (i/input {:type "text" :placeholder "Login" :id "login"})
      (i/input {:type "password" :placeholder "Password" :id "password"})
      (b/button {:bs-style "primary"
                 :on-click login} "Login")))))

(defn nav-style [app section view]
  (if (and (= section (:section app)) (= view (:view app)))
    #js{:className "active"}
    #js{}))


(def addable?
  #{:vms :users :roles :orgs :packages :networks :ipranges :dtrace})

(defn nav-bar [app]
  (n/navbar
   {:brand (d/a {:href (str "#/")} "FiFo")}
   (n/nav
    {:collapsible? true}
    (n/nav-item {:key 1 :href "#/vms"} "Machines")
    (n/nav-item {:key 2 :href "#/datasets"} "Datasets")
    (n/nav-item {:key 3 :href "#/hypervisors"} "Hypervisors")
    (b/dropdown {:key 4 :title "Configuration"}
                (b/menu-item {:key 1 :href "#/users"} "Users")
                (b/menu-item {:key 2 :href "#/roles"} "Roles")
                (b/menu-item {:key 3 :href "#/orgs"} "Organisations")
                (b/menu-item {:divider? true})
                (b/menu-item {:key 4 :href "#/packages"} "Packages")
                (b/menu-item {:key 5 :href "#/networks"} "Networks")
                (b/menu-item {:key 6 :href "#/ipranges"} "IP Ranges")
                (b/menu-item {:key 6 :href "#/dtrace"} "Dtrace")))))


(defn main-view [app]
  (g/grid
   nil
   (g/row
    nil
    (g/col
     {:xs 18 :md 12}
     (match
      (:section app)
      :vms         (vms/render app)
      :datasets    (datasets/render app)
      :hypervisors (hypervisors/render app)
      :networks    (networks/render app)
      :packages    (packages/render app)
      :ipranges    (ipranges/render app)
      :dtrace      (dtrace/render app)
      :users       (users/render app)
      :roles       (roles/render app)
      :orgs        (orgs/render app)
      :else        (goto "/vms"))))))


(defn add-btn [app]
  (g/row
   {:id "add-btn"}
   ;; menu-up
   ;; menu-down
   ;; glyphicon-plus
   (g/col {:xs 2 :xs-offset 5 :style {:text-align " center"}}
          (match
           (conf/get-config [:add :state] "none")
           "maximised" (r/glyphicon {:glyph "menu-down" :on-click #(conf/set-config! [:add :state] "minimised")})
           "minimised" (r/glyphicon {:glyph "menu-up" :on-click #(conf/set-config! [:add :state] "maximised")})
           :else (if (addable? (:section app))
                   (r/glyphicon {:glyph "plus" :class "createicon" :on-click
                                 (fn []
                                   (do
                                     (conf/set-config! [:add :section] (name (:section app)))
                                     (conf/set-config! [:add :state] "maximised")))}))))))


(defn submit-add [app]
  (if (conf/get-config [:add :valid] false)
    (let [section (conf/get-config [:add :section])
          data (conf/get-config [:add :data])]
      (go
        (let [resp (<! (http/post section {} {:json-params data}))]
          (if (:success resp)
            (do
              (pr "success" resp)
              (goto (str "/" section "/" (:uuid (:body resp))))
              (conf/delete-config! :add))
            (pr "success" resp)))))
    (pr "invalid "(conf/get-config [:add :data]))))

(def add-renderer
  {"vms"      vms-create/render
   "users"    users-create/render
   "roles"    roles-create/render
   "orgs"     orgs-create/render
   "packages" packages-create/render
   "networks" networks-create/render
   "ipranges" ipranges-create/render
   "dtrace"   dtrace-create/render})
(def add-title
  {"vms"      "Create VM"
   "users"    "Create User"
   "roles"    "Create Role"
   "orgs"     "Create Organisation"
   "packages" "Create Package"
   "networks" "Create Network"
   "ipranges" "Create IP-Range"
   "dtrace"   "Create DTrace Script"})

(defn add-body [app]
  (if (= (conf/get-config [:add :state]) "maximised")
    (if-let [section (conf/get-config [:add :section] "vms")]
      (if-let [create-view (add-renderer section)]
        (g/row
         {:id "add-body"}
         (g/col
          {:md 12 :style {:text-align "center"}}
          (d/h4 {:style {:padding-left "38px"}} ;; padding to compensate for the two icons on the right
                (add-title section)
                (r/glyphicon {:glyph "remove" :class "pull-right" :on-click #(conf/delete-config! :add)})
                (r/glyphicon {:glyph "ok" :class "pull-right" :on-click #(submit-add app)}))
          (create-view app)))))))

(defn add-view [app]
  (g/grid
   {:id "add-view"}
   (add-btn app)
   (add-body app)))

(defn main []
  (om/root
   (fn [app owner]
     (om/component
      (if (:token app)
        (d/div
         {}
         (nav-bar app)
         (main-view app)
         (add-view app))
        (do (goto)
            (login app)))))
   app-state
   {:target (by-id "app")}))
