(defproject guestbook "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[ch.qos.logback/logback-classic "1.4.5"]
                 [clojure.java-time "1.1.0"]
                 [com.h2database/h2 "2.1.214"]
                 [conman "0.9.6"]
                 [cprop "0.1.19"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 ;;
                 ;; Upgrade transitive dep of funcool/struct: funcool/cuerdas
                 ;; from 2.2.0 to 2.2.1
                 ;; in an attempt to fix the followin warning:
                 ;; parse-double already refers to: cljs.core/parse-double being replaced by: cuerdas.core/parse-double
                 ;; which, sadly, did not fix it.
                 [funcool/cuerdas "2.2.1"]
                 ;;
                 [json-html "0.4.7"]
                 [luminus-http-kit "0.2.0"]
                 [luminus-migrations "0.7.5"]
                 [luminus-transit "0.1.5"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.11.4"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.18"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.17"]
                 [nrepl "1.0.0"]
                 ;; Not putting "provided" between double quotes results in a really vague error message like:
                 ;; "class clojure.lang.Symbol cannot be cast to class java.lang.String" "clojure.lang.Symbol is in unnamed module of loader "
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 ;; reagent 1.0.0 has transitive dependencies for cljsjs/react and react-dom,
                 ;; the newer version apparently does not: specify them here explicitly
                 [reagent "1.1.1"]
;;                 [cljsjs/react "18.2.0-1"]
;;                 [cljsjs/react-dom "18.2.0-1"]
                 [cljs-ajax "0.8.4"]
                 [re-frame "1.3.0"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.214"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.webjars.npm/bulma "0.9.4"]
                 [org.webjars.npm/material-icons "1.10.8"]
                 [org.webjars/webjars-locator "0.45"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [selmer "1.12.55"]
                 ;; After migration from cljs-build to shadow-cljs
                 [com.google.javascript/closure-compiler-unshaded "v20221102" :scope "provided"]
                 [org.clojure/google-closure-library "0.0-20191016-6ae1f72f" :scope "provided"]
                 [thheller/shadow-cljs "2.20.16" :scope "provided"]
                 ]


  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot guestbook.core

  :plugins []

  :profiles
  {:uberjar       {:omit-source    true
                   :aot            :all
                   :uberjar-name   "guestbook.jar"
                   :source-paths   ["env/prod/clj"]
                   :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev   {:jvm-opts       ["-Dconf=dev-config.edn"]
                   :dependencies   [[binaryage/devtools "1.0.2"] ;; cljs-devtools
                                    [org.clojure/tools.namespace "1.3.0"]
                                    [pjstadig/humane-test-output "0.11.0"]
                                    [prone "2021-04-23"]
                                    [ring/ring-devel "1.9.6"]
                                    [ring/ring-mock "0.4.0"]
                                    [day8.re-frame/re-frame-10x "1.5.0"]
                                    ]
                   :plugins        [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                    [jonase/eastwood "1.2.4"]
                                    [cider/cider-nrepl "0.26.0"]]

                   :source-paths   ["env/dev/clj" "env/dev/cljc" "env/dev/cljs"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options   {:init-ns user
                                    :timeout 120000}
                   :injections     [(require 'pjstadig.humane-test-output)
                                    (pjstadig.humane-test-output/activate!)]}
   :project/test  {:jvm-opts       ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}
   :profiles/dev  {}
   :profiles/test {}})
