(ns s3atom
  (:require [promesa.core :as p]
            [cljs.reader :as reader]
            [cljs.core :refer [IPrintWithWriter IDeref IReset ISwap]]
            ["aws-sdk" :as AWS])
  (:require-macros [promesa.core]))

(defn -s3-bucket [aws bucket-name]
  (new (.-S3 aws) (clj->js {:apiVersion "2006-03-01"
                            :params     {:Bucket bucket-name}})))

(defn -upload-file [aws bucket-name item-key blob]
  (let [s3 (-s3-bucket aws bucket-name)]
    (p/promise (fn [resolve reject]
                 (.upload s3 #js {:Key  item-key
                                  :Body blob
                                  :ACL  "public-read"}
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

(deftype S3Atom [aws bucket-name item-key]
  IPrintWithWriter
  (-pr-writer [o writer _]
    (-write writer (str "#<S3Atom:s3://" bucket-name "/" item-key ">"))))

(extend-type S3Atom
  IDeref
  (-deref [o]
    (-> (-download-file (.-aws o) (.-bucket-name o) (.-item-key o))
        (p/then #(.toString % "utf-8"))
        (p/then -deserialize)))

  IReset
  (-reset! [o new-value]
    (-> (-upload-file (.-aws o) (.-bucket-name o) (.-item-key o) (-serialize new-value))
        (p/then #(deref o))))

  ISwap
  (-swap!
    ([o f]
     (p/alet [oldval (p/await (deref o))
              newval (f oldval)]
             (reset! o newval)))
    ([o f a]
     (p/alet [oldval (p/await (deref o))
              newval (f oldval a)]
             (reset! o newval)))
    ([o f a b]
     (p/alet [oldval (p/await (deref o))
              newval (f oldval a b)]
             (reset! o newval)))
    ([o f a b xs]
     (p/alet [oldval (p/await (deref o))
              newval (apply f oldval a b xs)]
             (reset! o newval)))))

(defn s3-atom
  ([aws-config bucket-name item-key]
   (let [_ (AWS/config.update (clj->js aws-config))]
     (S3Atom. AWS (name bucket-name) (name item-key)))))
