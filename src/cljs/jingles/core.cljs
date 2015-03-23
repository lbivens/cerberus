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

            [jingles.vms :as vms]
            [jingles.datasets :as datasets]
            [jingles.hypervisors :as hypervisors]
            [jingles.packages :as packages]
            [jingles.networks :as networks]
            [jingles.ipranges :as ipranges]
            [jingles.dtrace :as dtrace]
            [jingles.users :as users]
            [jingles.roles :as roles]
            [jingles.orgs :as orgs]

            [jingles.list :as jlist]
            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state app-alerts set-alerts! set-state!]]))

(enable-console-print!)

(set-state! :text "Hello Chestnut!")

(if-let [token (.get goog.net.cookies "token")]
  (set-state! :token token))

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

(defn main []
  (om/root
   (fn [app owner]
     (om/component
      (if (:token app)
        (d/div
         {}
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
                       (b/menu-item {:key 6 :href "#/dtrace"} "Dtrace"))))
         (g/grid
          nil
          (g/row
           nil
           (g/col
            {:md 12}
            (match
             (:section app)
             :vms (vms/render app)
             :datasets (datasets/view app)
             :hypervisors (hypervisors/view app)
             :networks (networks/view app)
             :packages (packages/view app)
             :ipranges (ipranges/view app)
             :dtrace (dtrace/view app)
             :users (users/view app)
             :roles (roles/render app)
             :orgs (orgs/render app)
             :else    (goto "/vms"))))))
        (do (goto)
            (login app)))))
   app-state
   {:target (by-id "app")}))
