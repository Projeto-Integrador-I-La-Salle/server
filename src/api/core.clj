(ns api.core
  (:require [org.httpkit.server :as http]
            [cheshire.core :as json]
            [api.handler :as handler]
            [compojure.core :refer  [defroutes GET POST PATCH]])
  (:gen-class))

(defn wrap-cors [handler]
  (fn [req]
    (let [cors-headers {"Access-Control-Allow-Origin"  "*"
                        "Access-Control-Allow-Methods" "GET, POST, PATCH, DELETE, OPTIONS"
                        "Access-Control-Allow-Headers" "Content-Type, Authorization"}]
      
      (if (= :options (:request-method req))
        {:status 200
         :headers cors-headers
         :body ""}
        
        (let [resp (handler req)]
          (update resp :headers merge cors-headers))))))

(defroutes app
  (GET   "/"               []   {:status 200 :body "Home"})
  (GET   "/health"         []   {:status 200 :body "OK"})
  (POST  "/products"       req  (handler/products-handler req))
  (GET   "/products"       []   (handler/get-all-products))
  (GET   "/products/:id"   [id] (handler/get-by-aggregate-id id))
  (PATCH "/products/:id"   req
         (let [id (get-in req [:path-params :id])]
           (handler/update-products-handler id)))
  (GET "/images/:filename" [filename] (handler/get-image filename))
  (fn                      [_]  {:status 404 :body "Not Found"}))

(def app-with-cors
  (wrap-cors app))

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

(defn -main []
  (start!))
