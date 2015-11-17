(defproject clutter "1.0.0-SNAPSHOT"
  :description "An experiment"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"
                  :exclusions [org.clojure/core.cache]]

                 [com.cemerick/friend "0.2.1" :exclusions [org.clojure/core.cache]]
                 [clojail "1.0.6"]
                 [com.taoensso/timbre "4.1.4"]
                 [jarohen/chord "0.6.0"]
                 [ring "1.4.0"]
                 [environ "1.0.1"]
                 [ring/ring-core "1.3.2"]
                 [ring-transit "0.1.4"]
                 [http-kit "2.1.18"]
                 [compojure "1.4.0"]
                 [enlive "1.1.6"]

                 [org.omcljs/om "0.9.0"]
                 [sablono "0.3.6"]

                 ;; JS:
                 [cljsjs/codemirror "5.7.0-1"]

                 [com.stuartsierra/component "0.3.0"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT"]
                 [org.clojure/tools.reader "0.9.2"]
                 [cljs-http "0.1.37"]
                 [ring/ring-json "0.4.0"]]

  :main clutter.core

  :repl-options {:init-ns clutter.repl}

  :source-paths ["src/clj" "src/cljc"]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-doo "0.1.6-SNAPSHOT"]
            [cider/cider-nrepl "0.9.1"]]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel {:on-jsload "clutter.core/on-js-reload"}
                        :compiler {:main clutter.core
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/clutter.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :preamble []
                                   :source-map true}}
                       {:id "release"
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler {:main clutter.core
                                   :output-to "resources/public/js/clutter.js"
                                   :optimizations :advanced
                                   :preamble []
                                   :pretty-print false}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "resources/public/js/testable.js"
                                   :main clutter.runner
                                   :optimizations :none
                                   :target :nodejs}}]}

  :figwheel {:server-port 3447
             :nrepl-port 3448
             :http-server-root "public"

             :css-dirs ["resources/public/css"]}

  :profiles {:dev {:plugins [[lein-figwheel "0.5.0-SNAPSHOT"]
                             [refactor-nrepl "1.1.0"]]
                   :env {:dev true
                         :port 3338}}})
