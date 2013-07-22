(defproject appengy-simple "0.1.9"
  :description "Simple appengy server"
  :url "http://github.com/galdolber/appengy-simple"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot :all
  :main appengy.AppengySimple
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [appengy "0.1.9"]
                 [org.clojure/tools.cli "0.2.2"]
                 [ring/ring-devel "1.2.0-beta2"]])
