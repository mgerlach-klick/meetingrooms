(ns aws
  (:require [promesa.core :as p]
            [cljs.reader :as reader]
            [cljs.core :refer [IPrintWithWriter IDeref IReset ISwap]]
            ["aws-sdk" :as AWS]
            [config]
            [clojure.string :as str])
  (:require-macros [promesa.core]))

(defn configure-aws [aws-config]
  (AWS/config.update (clj->js aws-config))
  AWS)


(defn -s3-bucket [aws bucket-name]
  (new (.-S3 aws) (clj->js {:apiVersion "2006-03-01"
                            :params     {:Bucket bucket-name}})))

(defn -upload-file [aws bucket-name item-key blob content-type]
  (let [s3 (-s3-bucket aws bucket-name)]
    (p/promise (fn [resolve reject]
                 (.upload s3 #js {:Key  item-key
                                  :Body blob
                                  :ACL  "public-read"
                                  :ContentType content-type}
                          (fn [err data]
                            (if err
                              (reject (js->clj err))
                              (resolve (js->clj data)))))))))

(defn -download-file [aws bucket-name item-key]
  (let [s3 (-s3-bucket aws bucket-name)]
    (p/promise (fn [resolve reject]
                 (.getObject s3 #js {:Key    item-key
                                     :Bucket bucket-name}
                             (fn [err data]
                               (if err
                                 (reject (js->clj err))
                                 (resolve (.-Body data)))))))))

(defn -serialize [v]
  (binding [*print-dup*      true
            *print-readably* true]
    (pr-str v)))

(def -deserialize
  (memoize reader/read-string))


(defn upload-edn&
  [bucket-name key new-value]
  (-upload-file AWS bucket-name key (-serialize new-value) "text/plain"))

(defn download-edn&
  [bucket-name item-key]
  (-> (-download-file AWS bucket-name item-key)
      (p/then #(.toString % "utf-8"))
      (p/then -deserialize)))

(defn upload-image&
  [bucket-name key blob]
  (-upload-file AWS bucket-name key blob "image/png"))

(defn uniquify-name [namestr]
  (-> namestr
      (str "-" (js/Date.) "-" (random-uuid))
      (str/replace #"[^A-Za-z0-9-_.]" "_")))

(defn update-rooms& [{:keys [rooms-file bucket-name]} rooms-edn]
  (p/all [(upload-edn& bucket-name rooms-file rooms-edn)
          (upload-edn& bucket-name (str "backups/" (uniquify-name rooms-file)) rooms-edn)]))
