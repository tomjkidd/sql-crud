(defproject org.clojars.tomjkidd/sql-crud "0.1.0-SNAPSHOT"
  :description "A library to perform simple SQL CRUD"
  :url "https://github.com/tomjkidd/sql-crud"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.xerial/sqlite-jdbc "3.14.2"]]
  :profiles
  {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.10"]]
         :source-paths ["dev"]
         :repl-options {:init-ns user}}})
