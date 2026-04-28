(ns api.case
  (:require [clojure.walk :as walk]
            [camel-snake-kebab.core :as csk]))

(defn ->kebab-case [data]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (-> x name csk/->kebab-case-keyword)
       x))
   data))

(defn ->camel-case [data]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (-> x name csk/->camelCase keyword)
       x))
   data))

