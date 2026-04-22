(ns api.dowloader
  (:require [clj-http.client :as client]
            [clojure.java.io :as io])
  (:import [javax.imageio ImageIO]
           [java.io ByteArrayInputStream]))

(defn safe-url? [url]
  (not (re-find #"localhost|127\.0\.0\.1|10\.|192\.168\." url)))

(defn extension-from-content-type [ct]
  (cond
    (re-find #"png" ct) ".png"
    (re-find #"jpeg" ct) ".jpg"
    (re-find #"jpg" ct) ".jpg"
    :else ".bin"))

(defn valid-image-bytes? [bytes]
  (try
    (let [img (ImageIO/read (ByteArrayInputStream. bytes))]
      (not (nil? img)))
    (catch Exception _ false)))

(defn download-image-safe [url]
  "Downloads the provided image url into uploads folder."
  (when-not (safe-url? url)
    (throw (ex-info "URL não permitida" {})))

  (let [response (client/get url {:as :byte-array
                                  :throw-exceptions false
                                  :socket-timeout 3000
                                  :conn-timeout 2000
                                  :max-body 5000000}) ;; 5mb
        status (:status response)
        headers (:headers response)
        bytes (:body response)
        content-type (get headers "content-type")]

    (when (and (= status 200)
               content-type
               (re-find #"^image/" content-type)
               (valid-image-bytes? bytes))

      (let [ext (extension-from-content-type content-type)
            filename (str "uploads/" (java.util.UUID/randomUUID) ext)]

        (.mkdirs (io/file "uploads"))

        (with-open [out (io/output-stream filename)]
          (.write out bytes))

        filename))))
