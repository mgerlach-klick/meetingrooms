(ns utils
  (:require [promesa.core :as p])
  (:require-macros [promesa.core]))

(defn resize [image-file max-width max-height cb]
  "Resizes the image to the max-width, max-height. Callback takes (blob: Blob)"
  (when-not js/ImageTools
    (throw (js/Error. "Please get the imagetools gist from https://gist.github.com/dcollien/312bce1270a5f511bf4a/raw/9bb680a9d30f0df8046a78f7335abfaf5c026135/ImageTools.js")))

  (.resize js/ImageTools
           image-file
           #js {:width max-width :height max-height}
           (fn [blob _]
             (let [new-file (js/File. [blob] "file")]
               (set! (.-type new-file) (.-type image-file))
               (set! (.-name new-file) (.-name image-file))
               (set! (.-lastModified new-file) (js/Date.))
               (cb new-file)))))

(defn resize& [image-file max-width max-height]
  (p/promise (fn [resolve reject]
               (resize image-file max-width max-height resolve))))


(defn nthify
  "Handle the weird 1st, 2nd, 3rd thing in English"
  [n]
  (when n
    (str n
         (let [rem (mod n 100)]
           (if (and (>= rem 11) (<= rem 13))
             "th"
             (condp = (mod n 10)
               1 "st"
               2 "nd"
               3 "rd"
               "th"))))))

(defn trim [s]
  (cond
    (nil? s)     nil
    (string? s)  (str/trim s)
    (keyword? s) (-> s name)
    :else        s))

(defn print-promise
  "For internal use only"
  [p]
  (-> p
      (p/then #(prn 'SUCCESS %))
      (p/catch #(prn 'FAIL %))))
