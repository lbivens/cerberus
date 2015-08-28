(ns cerberus.add
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.dom :as d :include-macros true]
   [om-bootstrap.input :as i]
   [om-bootstrap.random :as r]
   [om-bootstrap.button :as b]
   [om-bootstrap.grid :as g]
   [cerberus.debug :as dbg]
   [cerberus.api :as api]
   [cerberus.config :as conf]
   [cerberus.utils :refer [goto]]
   [cerberus.packages.create :as packages]
   [cerberus.networks.create :as networks]
   [cerberus.ipranges.create :as ipranges]
   [cerberus.users.create :as users]
   [cerberus.dtrace.create :as dtrace]
   [cerberus.orgs.create :as orgs]
   [cerberus.clients.create :as clients]
   [cerberus.roles.create :as roles]
   [cerberus.datasets.create :as datasets]
   [cerberus.vms.create :as vms]))

(def add-renderer
  {:vms      vms/render
   :users    users/render
   :roles    roles/render
   :orgs     orgs/render
   :clients  clients/render
   :packages packages/render
   :networks networks/render
   :ipranges ipranges/render
   :dtrace   dtrace/render
   :datasets datasets/render})

(def add-title
  {:vms      "Create VM"
   :users    "Create User"
   :roles    "Create Role"
   :orgs     "Create Organisation"
   :clients  "Create Client"
   :packages "Create Package"
   :networks "Create Network"
   :ipranges "Create IP-Range"
   :dtrace   "Create DTrace Script"
   :datasets "Import Dataset"})

(def submit-text
  {:vms      "Create"
   :users    "Add User"
   :roles    "Add Role"
   :orgs     "Add Organisation"
   :clients  "add Client"
   :packages "Add Package"
   :networks "Add Network"
   :ipranges "Add IP-Range"
   :dtrace   "Add DTrace Script"
   :datasets "Import Dataset"})

(def add-submit
  {:datasets datasets/submit})

(defn submit-default [section data]
  (api/post (keyword section) [] data))

(defn clear-add [data]
  (let [section (:view-section data)]
    (om/transact! data  (constantly {:view-section section}))))

(defn submit-add [data]
  (let [values (get-in data [:content :data])]
    (if (get-in data [:content :valid])
      (let [section (:section data)
            submit-fn (get-in add-submit [section] submit-default)]
        (dbg/debug "[add] valid values " (:section data) "valid " values)
        (if (submit-fn section values)
          (clear-add data)))
      (dbg/info "[add] invalid values " values))))

(defn init-add [data section]
  (om/transact! data :section (constantly section))
  (om/transact! data :content (constantly {}))
  (om/transact! data :maximized (constantly true)))

(defn add-btn [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "addbtnc")
    om/IRenderState
    (render-state [_ _]
      (let [maximized (:maximized data)
            view-section (:view-section data)
            addable (boolean (add-title view-section))]
        (g/row
         {:id "add-ctrl"}
         (g/col
          {:xs 2 :xs-offset 5 :style {:text-align "center"}}
          (match
           maximized
           true (r/glyphicon {:glyph "menu-down" :on-click #(om/transact! data :maximized (constantly false))})
           false (r/glyphicon {:glyph "menu-up" :on-click #(om/transact! data :maximized (constantly true))})
           :else (if addable
                   (r/glyphicon {:glyph "plus" :id "add-plus-btn" :on-click #(init-add data view-section)}))))
         (g/col
          {:class "addicons"}
          (if (not (nil? maximized))
            (r/glyphicon {:glyph "remove" :class "pull-right" :on-click #(clear-add data)}))
          (if (and maximized (not (:stash data)))
            (r/glyphicon
             {:glyph "cloud-upload" :class "pull-right" :id "add-stash-btn"
              :on-click
              #(let [add (conf/get [:add])]
                 (conf/delete! :add)
                 (conf/write! [:stash] add)
                 (init-add data view-section))}))))))))

(defn add-body [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "addbodyc")
    om/IRenderState
    (render-state [_ _]
      (d/div
       {:id "add-body"}
       (if-let [section (:section data)]
         [(g/row
           {:id "add-hdr"}
           (if-let [create-view (add-renderer section)]
             (g/col
              {:md 12 :style {:text-align "center"}}
              (d/h4
               (add-title section)
               (b/toolbar
                {}
                (b/button
                 {:bs-style "primary"
                  :class (str "pull-right createbutton"
                              (if (get-in data [:content :valid])
                                " valid"
                                " invalid"))
                  :disabled? (get-in data [:content :valid])
                  :on-click #(submit-add data)} (submit-text section)))))))
          (g/row
           {:id "add-content"}
           (if-let [create-view (add-renderer section)]
             (g/col
              {:md 12}
              (om/build create-view (:content data)))))])))))

(defn render [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "addc")
    om/IRenderState
    (render-state [_ _]
      (g/grid
       {:id "add-view"
        :class (if (:maximized data) "add-open" "add-closed")}
       (om/build add-btn data)
       (om/build add-body data)))))
