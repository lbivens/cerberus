(ns jingles.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [cljs-http.client :as httpc]
            [cljs.core.match]
            [om.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [jingles.routing]
            [jingles.http :as http]
            [jingles.vms.list :as vm-list]
            [jingles.datasets.list :as dataset-list]
            [jingles.servers.list :as server-list]
            [jingles.list :as jlist]
            [jingles.utils :refer [goto val-by-id by-id a]]
            [jingles.state :refer [app-state app-alerts set-alerts! set-state!]]
            [om-bootstrap.input :as i]))

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
                              token (e "access_token")]
                          (swap! app-state assoc :account token)
                          (goto)
                          (set-state! :token token))))))]
    (r/well
     {:style {:max-width 400
              :margin "300px auto 10px"}}
     (d/form
      nil
      (i/input {:type "text" :placeholder "Login" :id "login"})
      (i/input {:type "password" :placeholder "Password" :id "password"})
      (b/button {:bs-style "primary"
                 :on-click login} "Login")))))

(defn main []
  (om/root
   (fn [app owner]
     (om/component
      (if (:token app)
        (d/div
         #js{}
         (d/div
          #js{:className "container"}
          (d/div
           #js{:className "navbar-header"}
           (d/a #js{:href (str "#/") :className"navbar-brand"} "FiFo"))
          (d/nav
           #js{:className "bs-navbar-collapse navbar-collapse"}
           (d/ul
            #js{:className "nav navbar-nav"}
            (d/li nil (a "#/vms" "Machines"))
            (d/li nil (a "#/datasets" "Datasets"))
            (d/li nil (a "#/servers" "Servers"))
            )))
         (match
          (:view app)
          :vm-list (do (vm-list/full-list) (vm-list/render app))
          :dataset-list (do (dataset-list/full-list) (dataset-list/render app))
          :server-list (do (server-list/full-list) (server-list/render app))
          :else    (goto "/vms")))
        (do (goto)
            (login app)))))
   app-state
   {:target (by-id "app")}))
