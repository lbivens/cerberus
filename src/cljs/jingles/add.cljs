(ns jingles.add
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
   [om-bootstrap.random :as r]
   ;[om-bootstrap.button :as b]
   [om-bootstrap.grid :as g]

   [jingles.api :as api]
   [jingles.config :as conf]
   [jingles.utils :refer [goto]]
   [jingles.packages.create :as packages]
   [jingles.networks.create :as networks]
   [jingles.ipranges.create :as ipranges]
   [jingles.users.create :as users]
   [jingles.dtrace.create :as dtrace]
   [jingles.orgs.create :as orgs]
   [jingles.roles.create :as roles]
   [jingles.datasets.create :as datasets]
   [jingles.vms.create :as vms]))


(def add-renderer
  {"vms"      vms/render
   "users"    users/render
   "roles"    roles/render
   "orgs"     orgs/render
   "packages" packages/render
   "networks" networks/render
   "ipranges" ipranges/render
   "dtrace"   dtrace/render
   "datasets" datasets/render})

(def add-title
  {"vms"      "Create VM"
   "users"    "Create User"
   "roles"    "Create Role"
   "orgs"     "Create Organisation"
   "packages" "Create Package"
   "networks" "Create Network"
   "ipranges" "Create IP-Range"
   "dtrace"   "Create DTrace Script"
   "datasets" "Import Dataset"})

(def add-submit
  {"datasets" datasets/submit})


(defn submit-default [section data]
  (api/post (keyword section) data))

(defn submit-add [app]
  (if (conf/get [:add :valid] false)
    (let [section (conf/get [:add :section])
          submit-fn (get-in add-submit [(name section)] submit-default)]
      (if (submit-fn section (conf/get [:add :data]))
        (conf/delete! :add)))
    (pr "invalid " (conf/get [:add :data]))))

(defn add-btn [app]
  (g/row
   {:id "add-ctrl"}
   ;; menu-up
   ;; menu-down
   ;; glyphicon-plus
   (g/col {:xs 2 :xs-offset 5 :style {:text-align " center"}}
          (match
           (conf/get [:add :state] "none")
           "maximised" (r/glyphicon {:glyph "menu-down" :on-click #(do (conf/write! [:add :state] "minimised")
                                                                       (conf/flush!))})
           "minimised" (r/glyphicon {:glyph "menu-up" :on-click #(conf/write! [:add :state] "maximised")})
           :else (if (add-title (name  (:section app)))
                   (r/glyphicon {:glyph "plus" :id "add-plus-btn" :on-click
                                 (fn []
                                   (do
                                     (conf/write! [:add :section] (name (:section app)))
                                     (conf/write! [:add :state] "maximised")))}))))))




(defn add-body [app]
  [(g/row
    {:id "add-hdr"}
    (if (= (conf/get [:add :state]) "maximised")
      (if-let [section (conf/get [:add :section] "vms")]
        (if-let [create-view (add-renderer section)]
          (g/col
           {:md 12 :style {:text-align "center"}}
           (d/h4 {:style {:padding-left "38px"}} ;; padding to compensate for the two icons on the right
                 (add-title section)
                 (r/glyphicon {:glyph "remove" :class "pull-right" :on-click #(conf/delete! :add)})
                 (r/glyphicon {:glyph "ok" :class "pull-right" :on-click #(submit-add app)})))))))
   (g/row
    {:id "add-body" :style {:max-height "300px"
                            }}
    (if (= (conf/get [:add :state]) "maximised")
      (if-let [section (conf/get [:add :section] "vms")]
        (if-let [create-view (add-renderer section)]
          (g/col
           {:md 12}
           (create-view app))))))])

(defn render [app]
  (g/grid
   {:id "add-view"}
   (add-btn app)
   (add-body app)))
