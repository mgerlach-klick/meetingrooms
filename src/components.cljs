(ns components
  (:require [hiccups.runtime :as hiccupsrt]
            [config :refer [env]]
            [promesa.core :as p]
            [aws]
            [ui :refer [jquery execute-js! add-js set-html! on-event by-id] :as ui])
  (:require-macros [hiccups.core :refer [html]]
                   [promesa.core]))


(defn input-text
  "An input element which updates its value on change"
  [{:keys [id type value required placeholder readonly label atm transform-save transform-display]
    :or {transform-display identity
         transform-save identity}
    :as params}]
  (let [id      (or id (str (random-uuid)))
        valname (get params :name)
        kwname  (keyword valname)]

    (add-js (fn []
              (ui/on-event (ui/by-id id)
                           "change"
                           (fn [evt]
                             (swap! atm assoc kwname (transform-save (ui/event-val evt)))))))

    [:div.input-field
     [:input (merge {:id    (name id)
                     :name  (name (get params :name))
                     :class "validate"
                     :type  (name type)
                     :value (transform-display value)}
                    (when placeholder {:placeholder placeholder})
                    (when required {:required "required"})
                    (when readonly {:readonly "readonly"}))]
     (when label
       [:label {:for id :class "active"} label])

     [:span.helper-text {:id (str "helper-" (name id))}]]))

(defn input-textarea
  [{:keys [label id type value required readonly rows cols style atm placeholder] :as params}]
  (let [id      (or id (str (random-uuid)))
        valname (get params :name)
        kwname  (keyword valname)]
    (add-js (fn []
              (ui/on-event (ui/by-id id)
                           "change"
                           (fn [evt]
                             (swap! atm assoc kwname (ui/event-val evt))))))
    [:div.input-field
     [:textarea (merge {:id    (name id)
                        :name  (name valname)
                        :class "validate materialize-textarea"
                        :type  (name type)
                        :style style}
                       (when required {:required "required"})
                       (when readonly {:readonly "readonly"})
                       (when rows {:rows rows})
                       (when cols {:cols cols})
                       (when placeholder {:placeholder placeholder}))
      value]

     (when label
       [:label {:for id :class "active"} label])

     [:span.helper-text {:id (str "helper-" (name id))}]]))

(defn deletable-image
  [img-name img-src kw atm]
  (let [close-id (str (random-uuid))]
    (add-js (fn []
              (let [el (ui/by-id close-id)]
                (ui/on-click el
                             (fn []
                               (when (js/confirm "Are you sure you want to delete this picture?")
                                 (do
                                   (swap! atm update-in [kw] (fn [pics to-disj]
                                                               (disj (set pics) to-disj)) img-name)
                                   (-> el (.parent) (.fadeOut 300 #(.remove (.parent el)))))))))))

    [:div {:style "position: relative;"
           :class "deletable-image"}
     [:i.material-icons.close.noselect {:id close-id}
      "close"]
     [:img {:src   img-src
            :style "position: relative; max-width: 200px; max-height: 200px;"}]]))

(defn image-upload-area
  "Show current images and allow deletion. Add new images."
  [{:keys [atm kw id]}]
  (add-js ; just too lazy to translate
   " dropContainer = document.getElementById('drop-zone');
  dropContainer.ondragover = dropContainer.ondragenter = function(evt) {
    dropContainer.classList.add('mouse-over');
    evt.preventDefault();
  };

 dropContainer.ondragleave = function(evt) {
    dropContainer.classList.remove('mouse-over');
    evt.preventDefault();
  };


  dropContainer.ondrop = function(evt) {
    evt.preventDefault();
    dropContainer.classList.remove('mouse-over');
    Array.from(evt.dataTransfer.files).forEach(f => console.log(f.name))
  }; ")
  (add-js (fn []
            (on-event (by-id "drop-zone")
                      :drop
                      (fn [evt]
                        (.text (jquery "#drop-zone strong") "Uploading...")
                        (doseq [file (array-seq (.. evt -originalEvent -dataTransfer -files))]
                          (let [file-name (str (random-uuid) ".png")]
                            (p/then
                             (aws/upload-image& (env :bucket-name) (str "pics/" file-name) file)
                             (fn []
                               (swap! atm update-in [kw] conj file-name)
                               (ui/append-hiccup (ui/by-id "image-container")
                                                 (deletable-image file-name
                                                                  (str (env :s3url) file-name)
                                                                  :pictures
                                                                  atm))
                               (.text (jquery "#drop-zone strong") "Drop new image(s) here")))
                            (prn 'yayfile (.-name file) file-name)))))))
  [:div#drop-zone
   [:strong "Drop new image(s) here"]])
