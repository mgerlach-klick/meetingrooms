(ns components
  (:require [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))


(defn input-text
  "An input element which updates its value on change"
  [{:keys [id type value required placeholder readonly label] :as params}]
  (let [id (or id (str (random-uuid)))]
    [:span
     (when label
       [:label {:for id} label])
     [:input (merge {:id (name id)
                     :name (name (get params :name))
                     :class "form-control"
                     :type (name type)
                     :value value
                     :onchange "room[this.name] = this.value;"}
                    (when placeholder {:placeholder (if (and (= type "text")
                                                           placeholder)
                                                    placeholder
                                                    "")})
                    (when required {:required "required"})
                    (when readonly {:readonly "readonly"}))]]))

(defn input-textarea
  [{:keys [label id type value required readonly rows cols style] :as params}]
  (let [id (or id (str (random-uuid)))]
    [:span
     (when label
       [:label {:for id} label])
     [:textarea (merge {:id    (name id)
                        :name  (name (get params :name))
                        :class "form-control"
                        :type  (name type)
                        :style style
                        }
                       (when required {:required "required"})
                       (when readonly {:readonly "readonly"})
                       (when rows {:rows rows})
                       (when cols {:cols cols}))
      value]]))

(defn deletable-image
  [img-src]
  [:div {:style "position: relative;"}
   [:i.material-icons.close.noselect "close"]
  [:img {:src img-src
         :style "position: relative; max-width: 200px; max-height: 200px;"}]]
  )

(defn image-upload-area
  "Show current images and allow deletion. Add new images."
  []
  )
