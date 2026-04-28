(ns api.handler
  (:require [api.schemas            :as schemas]
            [api.case               :as case]
            [cheshire.core          :as json]
            [clojure.spec.alpha     :as s]
            [monger.core            :as mg]
            [monger.collection      :as mc]
            [api.dowloader          :as d]
            [clojure.java.io        :as io])
  (:import [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]
           [java.time LocalDateTime]
           [java.nio.file Files]
           [java.net URLConnection]))

(defonce mongo-conn
  (mg/connect-via-uri "mongodb://root:root@localhost:27017/admin?authSource=admin"))

(defonce conn (:conn mongo-conn))
(defonce db   (mg/get-db conn "app"))

(defn parse-body [req]
  (-> req
      :body
      slurp
      (json/parse-string true)
      (case/->kebab-case)))

(defn json-response [status data]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (-> data
             (case/->camel-case)
             (json/generate-string))})

(defn insert!
  [db coll doc]
  (mc/insert db coll (case/->camel-case doc)))

(defn insert-returning!
  [db coll doc]
  (mc/insert-and-return db coll (case/->camel-case doc)))

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
    :id (:aggregate-id event)
    :created-at (:created-at event)}
   (:payload event)))

(defmethod apply-event :product/product-updated
  [state event]
  (merge state
         (:payload event)
         (:updated-at (:created-at event))))

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

(defn find-maps-kebab
  [db coll query]
  (->> (mc/find-maps db coll query)
       (map case/->kebab-case)))

(defn find-product [id]
  (->> (find-maps-kebab db "events" {:aggregate-id (str id)})
       (project)))

(defn get-by-aggregate-id
  [aggregate-id]
  (let [events (find-product aggregate-id)]
    (json-response 200
                   {:data (->> events
                               (serialize))})))
(defn products-handler
  "Saves a product."
  [req]
  (let [data (parse-body req)]
    (if (s/valid? ::schemas/product data)
      (let [data         (schemas/apply-defaults data)
            aggregate-id (ObjectId.)
            ;; Baixa imagens se vieram no payload
            images       (:images data)
            downloaded-images (when (seq images)
                                (->> images
                                     (map d/download-image-safe)
                                     (remove nil?)
                                     (vec)))
            data         (if downloaded-images
                           (assoc data :images downloaded-images)
                           data)]
        (try
          (let [doc (insert-returning! db "events"
                                       {:type           "product-created"
                                        :aggregate-id   (str aggregate-id)
                                        :aggregate-type "product"
                                        :payload        data
                                        :created-at     (str (LocalDateTime/now))})]
            (println "[INFO] Inserido:" doc)
            (json-response 201 {:id   (str aggregate-id)
                                :data data}))
          (catch Exception e
            (println "[ERROR]:" (.getMessage e))
            (json-response 500 {:error (.getMessage e)}))))

      (json-response 400 {:error   "Payload inválido"
                          :details (s/explain-str ::schemas/product data)}))))

(defn update-products-handler
  "Updates a product."
  [id req]
  (let [product (find-product id)
        data    (parse-body req)]
    (cond
      (empty? product)
      (json-response 404 {:error "Not Found"})

      (and (contains? data :price)
           (not (number? (:price data))))
      (json-response 400 {:error "invalid price format"})
      :else
      (let [images (:images data)
            downloaded-images (when (seq images)
                                (->> images
                                     (map d/download-image-safe)
                                     (remove nil?)
                                     (vec)))
            new-data (if downloaded-images
                       (assoc data :images downloaded-images)
                       data)]

        (insert! db "events"
                 {:type           "product-updated"
                  :aggregate-id   (str id)
                  :aggregate-type "product"
                  :payload        new-data
                  :created-at     (str (LocalDateTime/now))})

      (json-response 204 {})))))

;; get-image section
(defn safe-filename? [filename]
(not (re-find #"\.\." filename)))

(defn content-type [file]
(or (URLConnection/guessContentTypeFromName (.getName file))
    "application/octet-stream"))

(defn get-image [filename]
(if (not (safe-filename? filename))
  (json-response 400 {:error "Invalid filename"})

  (let [file (io/file "uploads" filename)]
    (if (and (.exists file) (.isFile file))
      {:status 200
       :headers {"Content-Type" (content-type file)
                 "Cache-Control" "public, max-age=31536000"} ;; 1 year
       :body (io/input-stream file)}

      (json-response 404 {:error "Not Found"})))))

(defn find-all-products
  [{:keys [brand name]}]
  (let [products (->> (find-maps-kebab db "events" {:aggregateType "product"})
                      (group-by :aggregate-id)
                      (vals)
                      (map project)
                      (map serialize))]
    (println "[DEBUG] primeiro produto:" (first products))
    (println "[DEBUG] params:" {:brand brand :name name})
    (cond->> products
      name (filter #(clojure.string/includes?
                     (clojure.string/lower-case (str (:name %)))
                     (clojure.string/lower-case name))))))

(defn get-all-products [req]
  (let [params (->> req :query-params
                    (map (fn [[k v]] [(keyword k) v]))
                    (into {}))]
    (json-response 200
                   {:content (find-all-products params)})))

;;(update-products-handler
;; "69dd8bc497d50f9dfbd26bb2"
;; {:body (java.io.ByteArrayInputStream.(.getBytes "{\"price\":12.39}"))})

;; Images  
;;(update-products-handler
;; "69dd8bc497d50f9dfbd26bb2"
;; {:body (java.io.ByteArrayInputStream.
;;         (.getBytes
;;           (json/generate-string
;;            {:images ["https://cdn1.staticpanvel.com.br

(update-products-handler
 "69e7e38897d50f19edb2a587"
 {:body (java.io.ByteArrayInputStream.
         (.getBytes
          (json/generate-string
           {:images ["https://cdn1.staticpanvel.com.br/produtos/15/94741-15_998246.jpg"]})))})

