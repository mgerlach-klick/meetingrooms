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
  [img-name img-src]
  [:div {:style "position: relative;"}
   [:i.material-icons.close.noselect
    {:onclick (str "if(confirm(\"Are you sure you want to delete this picture?\"))  {
  room[\"pictures\"] = room[\"pictures\"].filter(function(item){
      return item !== \"" img-name "\";
  });
$(this).parent().fadeOut(300, function(){$(this).remove()});
}")}
    "close"]
  [:img {:src img-src
         :style "position: relative; max-width: 200px; max-height: 200px;"}]]
  )

(defn image-upload-area
  "Show current images and allow deletion. Add new images."
  []
  [:div.box
   [:div.box__input
    [:input.box__file {:type "file" :name "files[]" :id "file" :multiple "multiple"}]
    [:label {:for "file"} [:strong "Choose a file"] "or drag it here"]
  ]
   [:div.box__uploading "Resizing & Uploading &hellip;"]
   [:div.box_error "Error!"]
   (.eval js/window "
console.log('image-upload-area');
$form = $(\"#file\");
var droppedFiles = false;

  $form.on('drag dragstart dragend dragover dragenter dragleave drop', function(e) {
    e.preventDefault();
    e.stopPropagation();
  })
  .on('dragover dragenter', function() {
    $form.addClass('is-dragover');
  })
  .on('dragleave dragend drop', function() {
    $form.removeClass('is-dragover');
  })
  .on('drop', function(e) {
    droppedFiles = e.originalEvent.dataTransfer.files;
    console.log(\"dropped files:\", droppedFiles);
  });")
   ]
  )
