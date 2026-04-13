(ns api.handler
  (:require [api.schemas        :as schemas]
            [cheshire.core      :as json]
            [clojure.spec.alpha :as s]
            [monger.core :as mg]
            [monger.collection :as mc ])
  (:import [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]
           [java.time LocalDateTime]))

(defonce mongo-conn
  (mg/connect-via-uri "mongodb://root:root@localhost:27017/admin?authSource=admin"))

(defonce conn (:conn mongo-conn))
(defonce db   (mg/get-db conn "app"))

(defn parse-body
  [req]
  (json/parse-string (slurp (:body req)) true))

(defn json-response
  [status data]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defmulti  apply-event
  (fn [_state event]
    (keyword
     (str (:aggregate-type event)
          "/"
          (:type event))))
  :default :unknown)

(defmethod apply-event :product/product-created
  [_state event]
  (merge
   {:resource-type (:aggregate-type event)
    :product-id (:aggregate-id event)
    :created-at (:created-at event)}
   (:payload event)))

(defn project
  ([events]
   (project {} events))
  ([state events]
   (reduce apply-event state events)))

(defn serialize [data]
  (clojure.walk/postwalk
    (fn [x]
      (if (instance? org.bson.types.ObjectId x)
        (str x)
        x))
    data))

(defn get-by-aggregate-id
  [aggregate-id]
  (let [events (mc/find-maps db "events" { :aggregate-id (ObjectId. (str aggregate-id))})]
    (json-response 200
                   {:data (->> events
                               (project)
                               (serialize))})))

(defn products-handler
  [req]
  (case (:request-method req)
    :get
    (json-response 200 {:data (get-by-aggregate-id)})

    :post
    (let [data (parse-body req)]
      (if (s/valid? ::schemas/product data)
        (let [data (schemas/apply-defaults data)
              aggregate-id (ObjectId.)]
          (try
            (let [doc (mc/insert-and-return db "events"
                                            {:type          "product-created"
                                             :aggregate-id   aggregate-id
                                             :aggregate-type "product"
                                             :payload       data
                                             :createdAt     (str (LocalDateTime/now))})]
              (println "[INFO] Inserido:" doc)
              (json-response 201 {:id      (str aggregate-id)
                                  :data    data}))
            (catch Exception e
              (println "[ERROR]:" (.getMessage e))
              (json-response 500 {:error (.getMessage e)}))))

        (json-response 400 {:error   "Payload inválido"
                            :details (s/explain-str ::schemas/product data)})))

    {:status 405 :body "Method Not Allowed"}))

