(ns api.schemas
  (:require [clojure.spec.alpha :as s])
  (:import [java.time LocalDateTime]))

(def product-defaults
  {:details ""
   :rating 0.0
   :sku ""
   :isDeleted false
   :images []
   :tags []
   :createdAt (str (LocalDateTime/now))
   :updatedAt (str (LocalDateTime/now))})

(defn apply-defaults
  [data]
  (merge product-defaults data))

(s/def ::name  string?)
(s/def ::price number?)
(s/def ::code number?)
(s/def ::quantity number?)
(s/def ::brand string?)

;; Optionals
(s/def ::details string?)
(s/def ::rating float?)
(s/def ::sku string?)
(s/def ::isDeleted boolean?)
(s/def ::createdAt string?)
(s/def ::updatedAt string?)

(s/def ::product
  (s/keys :req-un [::name
                   ::price
                   ::code
                   ::quantity
                   ::brand]))
