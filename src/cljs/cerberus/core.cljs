(ns cerberus.core
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [om.core :as om :include-macros true]
   [cljs-http.client :as httpc]
   [cljs.core.match]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.grid :as g]
   [om-bootstrap.input :as i]
   [om-bootstrap.nav :as n]
   [om-bootstrap.progress-bar :as pb]
   [cerberus.routing]
   [cerberus.http :as http]

   [cerberus.hypervisors :as hypervisors]
   [cerberus.datasets :as datasets]
   [cerberus.vms :as vms]
   [cerberus.packages :as packages]
   [cerberus.networks :as networks]
   [cerberus.ipranges :as ipranges]
   [cerberus.dtrace :as dtrace]
   [cerberus.users :as users]
   [cerberus.roles :as roles]
   [cerberus.orgs :as orgs]
   [cerberus.clients :as clients]
   [cerberus.config :as conf]
   [cerberus.add :as add]

   [cerberus.timers]
   [cerberus.utils :refer [goto val-by-id by-id a menu-items]]
   [cerberus.state :refer [app-state set-state!]]))

(enable-console-print!)

(def login-path "/api/2/oauth/token")

(defn login-fn []
  (go
    (let [req-body {:grant_type "password"
                    :username (val-by-id "login")
                    :password (val-by-id "password")}
          response (<! (httpc/post login-path {:form-params req-body
                                               :accept "application/json"}))]
      (if (= 200 (:status response))
        (let [e (:body response)
              token (e :access_token)
              expires-in (e :expires_in)]
          (conf/login token expires-in))))))

(defn login [app]
  (r/well
   {:id "login-box"}
   (d/form
    nil
    (i/input {:type "text" :placeholder "Login" :id "login"})
    (i/input {:type "password" :placeholder "Password" :id "password"
              :on-key-up #(if (= (.-keyCode  %) 13) (login-fn))})
    (b/button {:bs-style "primary"
               :on-click login-fn} "Login"))))

(defn nav-style [app section view]
  (if (and (= section (:section app)) (= view (:view app)))
    {:className "active"}
    {}))

(defn nav-bar [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "navbarc")
    om/IRenderState
    (render-state [_ _]
      (n/navbar
       {:brand (d/a {:href (str "#/")} "FiFo")}
       (n/nav
        {:collapsible? true}
        (n/nav-item {:key 1 :href "#/vms"} "Machines")
        (n/nav-item {:key 2 :href "#/datasets"} "Datasets")
        (n/nav-item {:key 3 :href "#/hypervisors"} "Hypervisors")
        (b/dropdown {:key 4 :title "Configuration"}
                    (menu-items
                     ["Users" "#/users"]
                     ["Roles"  "#/roles"]
                     ["Organisations"  "#/orgs"]
                     :divider
                     ["Clients"  "#/clients"]
                     :divider
                     ["Packages" "#/packages"]
                     :divider
                     ["Networks" "#/networks"]
                     ["IP Ranges" "#/ipranges"]
                     #_["DTrace" "#/dtrace"]
                     :divider
                     ["Logout" #(conf/logout)]
                     ["Logout & Reset UI" #(conf/clear)]))
        ;;Removed this for now
        #_(n/nav-item {:key 5 :style {:height 20 :width 200} :class "navbar-right hidden-xs hidden-sm"}
                    (pb/progress-bar {:min 0
                                      :max (get-in data [:total-memory] 0)
                                      :now (get-in data [:provisioned-memory] 0) :label "RAM"}))
        ;; Removed this for now
        #_(n/nav-item {:key 6 :style {:height 20 :width 200} :class "navbar-right hidden-xs hidden-sm"}
                    (pb/progress-bar {:min 0
                                      :max (get-in data [:disk-size] 0)
                                      :now (get-in data [:disk-used] 0) :label "Disk"})))
       ))))

(defn main-view [data]
  (condp = (:section data)
    :vms         (om/build vms/render data)
    :datasets    (om/build datasets/render data)
    :hypervisors (om/build hypervisors/render data)
    :networks    (om/build networks/render data)
    :packages    (om/build packages/render data)
    :ipranges    (om/build ipranges/render data)
    :dtrace      (om/build dtrace/render data)
    :users       (om/build users/render data)
    :roles       (om/build roles/render data)
    :orgs        (om/build orgs/render data)
    :clients     (om/build clients/render data)
    (goto "/vms")))

(defn main []
  (om/root
   (fn [app owner]
     (om/component
      (if (:token app)
        (d/div
         {:class (str "app " (if (get-in app [:add :maximized])  "add-open" "add-closed"))}
         (om/build nav-bar (get-in app [:cloud :metrics]))
         (g/grid
          {}
          (g/row
           {}
           (g/col
            {:xs 18 :md 12}
            (main-view app))))
         (om/build add/render (get-in app [:add])))
        (do (goto)
            (login app)))))
   app-state
   {:target (by-id "app")}))
