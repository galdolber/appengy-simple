(ns appengy-simple.core
  (:gen-class
   :name appengy.AppengySimple)
  (:import [java.io File]
           [java.text SimpleDateFormat]
           [java.util Date])
  (:require [org.httpkit.server :as http])
  (:use [appengy.httpkit :only [apps-handler clients-handler]]
        [appengy.socket :only [make-server]]
        [clojure.tools.cli :only [cli]]
        [clojure.java.shell :only [sh]]
        [ring.util.response :only [header]]
        appengy-simple.gzip
        ring.middleware.cookies
        ring.middleware.session
        ring.middleware.reload
        ring.middleware.session.cookie
        ring.middleware.file-info
        ring.middleware.multipart-params
        ring.middleware.params))

(def ^SimpleDateFormat cache-format (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss zzz"))

(defn add-cache-headers [res path]
  (cond
   (.contains ^String path ".cache.")
     (header res "Expires" (.format cache-format (+ (.getTime ^Date (Date.)) 31536000000N)))
   (.contains ^String path ".nocache.")
     (-> res
       (header "Pragma" "no-cache")
       (header "Cache-control" "no-store")
       (header "Expires" "-1"))
   :else res))

(defn wrap-cache-headers [app]
  (fn [req]
    (let [{:keys [body] :as res} (app req)]
      (if (instance? File body)
        (add-cache-headers res (.getCanonicalPath ^File body))
        res))))

(def pid-path "pid")

(defn safe-slurp [file]
  (try
    (slurp file)
    (catch Exception e nil)))

(defn pid []
  (-> (java.lang.management.ManagementFactory/getRuntimeMXBean)
    (.getName)
    (clojure.string/split #"@")
    (first)))

(defn kill-old []
  (if-let [p (safe-slurp pid-path)]
    (try
      (sh "kill" "-9" p)
      (catch Exception e nil)))
  (spit pid-path (pid)))

(defn start [port apps-port]
  (def ws
    (http/run-server
     (-> clients-handler
         wrap-cookies
         wrap-session
         wrap-file-info
         wrap-params
         wrap-multipart-params
         wrap-reload
         wrap-cache-headers
         wrap-gzip)
     {:port port}))
  (def apps-server (make-server apps-port apps-handler)))

(defn -main [& args]
  (let [[{:keys [port local apps-port]} _ usage]
        (cli args
             ["-ap" "--apps-port" "Apps port" :parse-fn #(Integer. %) :default 9090]
             ["-p" "--port" "Listen on this port" :parse-fn #(Integer. %) :default 8080]
             ["-l" "--local" "true for dev" :default true])]
    (kill-old)
    (start port apps-port)
    (println (str "Started server on localhost:" port))))
