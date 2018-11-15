(ns components
  (:require [hiccups.runtime :as hiccupsrt]
            [ui :refer [jquery execute-js! add-js set-html!]])
  )


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

(def ^:export my-atom (atom {}))
(defn ^:export confirm []
  (swap! my-atom assoc :floor (.val (js/$ "input[name='floor']")) :max :worked :time (js/Date.))
  (if (js/confirm "confirm me")
    (js/alert "yes")
    (js/alert "no")))

(defn ^:export printatom []
  (prn @my-atom))

(defn deletable-image
  [img-name img-src]
  (let [close-id (str (random-uuid))]
    (add-js (fn []
              (let [el (jquery (str "#" close-id))]
                (prn "installing click handler for " el)
                (.click el
                        (fn []
                          (when (js/confirm "Are you sure you want to delete this picture?")
                            (do
                              (swap! my-atom assoc :pictures (disj (set (:pictures @my-atom)) img-name))
                              (-> el (.fadeOut 300 #(.remove el))))))))))

    [:div {:id    close-id
           :style "position: relative;"}
     [:i.material-icons.close.noselect "close"]
     [:img {:src   img-src
            :style "position: relative; max-width: 200px; max-height: 200px;"}]])
  )

(defn image-upload-area
  "Show current images and allow deletion. Add new images."
  []
  (add-js (fn []
            (-> (.getElementById js/document "drop-zone")
                (.-ondragenter)
                (set! #(prn "Drag me, drag me :D")))))
  [:div#drop-zone
   [:strong "Drop image(s) here"]])
