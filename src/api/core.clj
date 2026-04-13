(ns api.core
  (:require [org.httpkit.server :as http]
            [cheshire.core :as json]
            [api.handler :as handler]
            [compojure.core :refer  [defroutes GET POST]])
  (:gen-class))

(defroutes app
  (GET "/" [] {:status 200 :body "Home"})
  (GET "/health" [] {:status 200 :body "OK"})
  (POST "/products" req (handler/products-handler req))
  (GET "/products/:id" [id] (handler/get-by-aggregate-id id))
  (fn [_] {:status 404 :body "Not Found"}))

(defonce server (atom nil))

(defn start! []
  (reset! server (http/run-server app {:port 3000}))
  (println "Server rodando em http://localhost:3000"))

(defn stop! []
  (when @server
    (@server) ; http-kit para o server chamando ele como função
    (reset! server nil)
    (println "Server parado")))

(defn restart! []
  (stop!)
  (start!))

(defn -main []
  (start!))

