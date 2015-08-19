(ns cerberus.dev
  (:require [environ.core :refer [env]]
            [net.cgrand.enlive-html :refer [set-attr prepend append html]]
            [figwheel-sidecar.auto-builder :as fig-auto]
            [figwheel-sidecar.core :as fig]
            [clojurescript-build.auto :as auto]
            [clojure.java.shell :refer [sh]]
            [leiningen.core.main :as lein]))

(def is-dev? (env :is-dev))

(def inject-devmode-html
  (comp
     (set-attr :class "is-dev")
     (prepend (html [:script {:type "text/javascript" :src "/js/out/goog/base.js"}]))
     (append  (html [:script {:type "text/javascript"} "goog.require('cerberus.main')"]))))

(defn start-figwheel []
  (let [server (fig/start-server { :css-dirs ["resources/public/css"] })
        config {:builds [{:id "dev"
                          :source-paths ["src/cljs" "env/dev/cljs"]
                          :compiler {:output-to            "resources/public/js/app.js"
                                     :output-dir           "resources/public/js/out"
                                     :source-map           "resources/public/js/out.js.map"
                                     :source-map-timestamp true
                                     :preamble             ["react/react.min.js"]}}]
                :figwheel-server server}]
    (fig-auto/autobuild* config)))

(defn start-less []
  (future
    (println "Starting less.")
    (lein/-main ["less" "auto" ])))
