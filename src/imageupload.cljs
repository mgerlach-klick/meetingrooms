(ns imageupload
  (:require [promesa.core :as p]
            ["aws-sdk" :as AWS])
  (:require-macros [promesa.core]))

(defn -s3-bucket [aws bucket-name]
  (new (.-S3 aws) (clj->js {:apiVersion "2006-03-01"
                            :params     {:Bucket bucket-name}})))

(defn -upload-file
  ([aws bucket-name item-key blob ]
   (-upload-file aws bucket-name item-key blob {:ContentType "image/png"}))
  ([aws bucket-name item-key blob params]
   (let [s3 (-s3-bucket aws bucket-name)]
     (p/promise (fn [resolve reject]
                  (.upload s3  (clj->js (merge params {:Key  item-key
                                                       :Body blob
                                                       :ACL  "public-read"}))
                           (fn [err data]
                             (if err
                               (reject (js->clj err))
                               (resolve (js->clj data))))))))))

(comment

  (defn -file-from-string [s type]
    (js/Blob. [s] #js {:type type}))

  (def blob (-> "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="
                (js/fetch)
                (p/then #(.blob %))))

  (defn upl []
    (-> blob
        (p/then #(-upload-file AWS main/bucket-name "test2.png" % {:ContentType "image/png"}))
        (main/print-promise)
        )))
