(ns jingles.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [cljs-http.client :as httpc]
            [jingles.http :as http]
            [om.dom :as d :include-macros true]
            [om-bootstrap.random :as r]
            [om-bootstrap.button :as b]
            [jingles.utils :refer [goto log val-by-id by-id]]
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
                          (goto "/")
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
        (do (go (let [resp (<! (http/get "/api/0.2.0/vms" {"x-full-list" "true"}))]
                  (pr  (:body resp))
                  (set-state! :list (str (:body resp)))))
            (d/h1 nil (str (:text app))))
        (do (goto)
            (login app)))))
   app-state
   {:target (by-id "app")}))
