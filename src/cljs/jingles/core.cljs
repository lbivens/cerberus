(ns jingles.core
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [om.core :as om :include-macros true]
   [goog.net.cookies :as cks]
   [cljs-http.client :as httpc]
   [cljs.core.match]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.grid :as g]
   [om-bootstrap.input :as i]
   [om-bootstrap.nav :as n]
   [om-bootstrap.progress-bar :as pb]
   [jingles.routing]
   [jingles.http :as http]

   [jingles.hypervisors :as hypervisors]
   [jingles.datasets :as datasets]
   [jingles.vms :as vms]
   [jingles.packages :as packages]
   [jingles.networks :as networks]
   [jingles.ipranges :as ipranges]
   [jingles.dtrace :as dtrace]
   [jingles.users :as users]
   [jingles.roles :as roles]
   [jingles.orgs :as orgs]
   [jingles.config :as conf]
   [jingles.add :as add]

   [jingles.timers]
   [jingles.utils :refer [goto val-by-id by-id a menu-items]]
   [jingles.state :refer [app-state set-state!]]))

(enable-console-print!)

(def login-path "/api/0.2.0/oauth/token")

(defn login-fn []
  (go
    (let [req-body {:grant_type "password"
                    :username (val-by-id "login")
                    :password (val-by-id "password")}
          response (<! (httpc/post login-path {:form-params req-body}))]
      (if (= 200 (:status response))
        (let [e (js->clj (. js/JSON (parse (:body response))))
              token (e "access_token")
              expires-in (e "expires_in")]
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
                     ["Packages" "#/packages"]
                     :divider
                     ["Networks" "#/networks"]
                     ["IP Ranges" "#/ipranges"]
                     ["DTrace" "#/dtrace"]
                     :divider
                     ["Logout" #(conf/logout)]
                     ["Logout & Reset UI" #(conf/clear)]))
        (n/nav-item {:key 5 :style {:height 20 :width 200} :class "navbar-right hidden-xs hidden-sm"}
                    (pb/progress-bar {:min 0
                                      :max (get-in data [:total-memory] 0)
                                      :now (get-in data [:provisioned-memory] 0) :label "RAM"}))
        (n/nav-item {:key 6 :style {:height 20 :width 200} :class "navbar-right hidden-xs hidden-sm"}
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
    ;; :ipranges    (ipranges/render data)
    ;; :dtrace      (dtrace/render data)
    ;; :users       (users/render data)
    ;; :roles       (roles/render data)
    ;; :orgs        (orgs/render data)
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
