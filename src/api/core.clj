(ns api.core
  (:require [org.httpkit.server :as http]
            [cheshire.core :as json]
            [api.handler :as handler]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer  [defroutes GET POST PATCH]])
  (:gen-class))

(defn wrap-cors [handler]
  (fn [req]
    (let [cors-headers {"Access-Control-Allow-Origin"  "*"
                        "Access-Control-Allow-Methods" "GET, POST, PATCH, OPTIONS"
                        "Access-Control-Allow-Headers" "Content-Type, Authorization"}]
      
      (if (= :options (:request-method req))
        {:status 200
         :headers cors-headers
         :body ""}
        
        (let [resp (handler req)]
          (update resp :headers merge cors-headers))))))

(defroutes app
  (GET   "/health"         []    {:status 200 :body "OK"})
  (POST  "/products"       req   (handler/products-handler    req))
  (GET "/products" req
     (do
       (println "[DEBUG] query-string:" (:query-string req))
       (println "[DEBUG] query-params:" (:query-params req))
       (println "[DEBUG] req keys:" (keys req))
       (handler/get-all-products req)))
  (GET   "/products/:id"   [id]  (handler/get-by-aggregate-id id))
  (PATCH "/products/:id"   req
         (let [id (get-in req [:path-params :id])]
           (handler/update-products-handler id)))
  (GET "/images/:filename" [filename] (handler/get-image filename))
  (fn                      [_]  {:status 404 :body "Not Found"}))

(def app-with-cors
  (wrap-cors app))

(def wrapped-app
  (-> app
      wrap-params
      wrap-cors))

(defonce server (atom nil))

(defn start! []
  (reset! server (http/run-server app-with-cors {:port 3000}))
  (println "Server rodando em http://localhost:3000"))

(defn stop! []
  (when @server
    (@server) ; http-kit para o server chamando ele como função
    (reset! server nil)
    (println "Server parado")))

(defn restart! []
  (stop!)
  (start!))

(defn -main [& args]
  (http/run-server wrapped-app {:port 3000}))
