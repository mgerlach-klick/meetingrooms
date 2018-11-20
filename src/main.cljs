(ns main
  (:require ["aws-sdk" :as AWS]
            [promesa.core :as p]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [components :refer [input-text input-textarea deletable-image image-upload-area input-switch]]
            [ui :refer [jquery execute-js! add-js set-html! ]]
            [clojure.string :as str]
            [config :refer [env] :as config]
            [aws])
  (:require-macros [promesa.core])
  (:import goog.history.Html5History))

(declare rooms
         application
         act-on-url!
         history
         hook-autocompleter!)


;; -------------------------
;; Utilities

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
  (when s
    (str/trim s)))

(defn print-promise
  "For internal use only"
  [p]
  (-> p
      (p/then #(prn 'SUCCESS %))
      (p/catch #(prn 'FAIL %))))


;; -------------------------
;; Room Manipulation

(defn get-room-data
  "Pull the room info out of the rooms database"
  [room-id]
  (get-in @rooms [:rooms (keyword room-id)]))

(defn update-room [room-id val]
  (swap! rooms assoc-in [:rooms (keyword room-id)] val ))

(defn room-names
  "Returns all the names of a given room, including the floor and tower"
  [{:keys [name aliases floor tower]}]
  (let [roomst (fn [name] (str name " (" (nthify floor) " " tower ")"))]
    (map roomst (cons name aliases))))

(defn room-name-to-id
  "Returns all possible room-name-to-id mappings of all the given rooms"
  [rooms]
  (into {}
        (for [[room-id room-val] rooms
              room-name          (room-names room-val)]
          (when-not (false? (:active room-val))
            [room-name room-id]))))



;; -------------------------
;; UI

(defn room-header
  [room]
  [:h4.header {:style "display: inline" } (:name room)
   [:br.hide-on-med-and-up]
   [:h5.grey-text  {:style "display: inline" }
    " ("
    (when (:floor room)
      (str (nthify (:floor room)) " "))
    (when (:tower room)
      (:tower room))
    ")"]])

(defn Room
  [{:keys [pictures aliases description moreinfo roomid] :as room}]
  (add-js (fn []
            (-> (jquery ".slider")
                (.slider (clj->js {"full_width" false
                                   "indicators" (-> pictures
                                                    count
                                                    (> 1))})))
            (js/window.scrollTo 0 0)))

  [:div (room-header room)

   (when-not (empty? aliases)
     [:p.grey-text "aka: " (apply str (interpose ", " aliases))])

   [:div.section
    [:p.flow-text description]]

   (when-not (empty? pictures)
     [:div.slider
      [:ul.slides
       (for [img pictures]
         [:li
          [:img {:src (str (env :s3url) img)}]])]])

   (when-not (empty? moreinfo)
     [:ul "More information:"
      (for [info moreinfo]
        [:li
         [:a {:href info} info ]])])

   [:hr]
   [:a.right {:href (str "#/room/" (name roomid) "/edit")}
    "Edit this page"]])


(defn Home []
  [:span
   [:p.section.flow-text "Just search for the meeting room by tapping on the search bar above! It supports fuzzy search and as soon as there is only one option left it will automatically load the result!"]
   [:p.section.flow-text "If you prefer a map with all the rooms take a look at the " [:a {:href "https://genome.klick.com/map/index.html#/"} "Seating Map"]]
   [:p.section.flow-text "Feel free to link directly to the URLs of the meeting rooms, I will keep the URLs stable."]
   [:p.section.flow-text "Please also help by contributing to this site by offering corrections, better instructions, additions, comments, and all that. There is a link through which you can email at the bottom of every page."]])

(defn validate-room-input
  [{:keys [roomid name tower floor aliases description last-updated] :as room}]
  (->> (conj []
             (when (empty? (trim roomid))
               "You must specify a room id!")
             ;; (when (and (get-room-data roomid)
             ;;            (not last-updated))
             ;;   "This room id already exists!")
             (when (empty? name)
               "Please enter a room name")
             (when (empty? (trim floor))
               "Please enter a floor")
             (when (empty? (trim description))
               "Please enter walking instructions. That's the whole point!"))
       (remove nil?)
       (seq)))

(defn Edit-Room
  [{:keys [roomid name tower floor aliases description pictures] :as room}]
  (let [atm (atom room)]


    (add-js (fn []
              (ui/on-click (ui/by-id :savebtn)
                           (fn [e]
                             (.preventDefault e)
                             (prn 'Updating roomid @atm)
                             (if-let [errors (validate-room-input @atm)]
                               (do
                                 (prn 'errors errors)
                                 (ui/alert :type "error"
                                           :title "Bad Input"
                                           :text (first errors)))
                               ;;else
                               (let [roomid         (get @atm :roomid)
                                     new-room-state (assoc @atm :last-updated (js/Date.))]
                                 (update-room roomid new-room-state)
                                 (.then
                                  (ui/alert :title "Saved!"
                                            :type "success"
                                            :showCancelButton false
                                            :showConfirmButton false
                                            :timer 1000)
                                  #(go-to-room roomid))))
                             false))))

    [:div
     [:form {:id :edit-form}

      (input-text {:label "Room-ID"
                   :name "roomid"
                   :placeholder "e.g. 'room1N' - The room name, floor and tower, to be unique!"
                   :value roomid
                   :required true
                   :readonly (when roomid true)
                   :type :text
                   :atm atm
                   :transform-save #(-> %
                                       (trim)
                                       (str/replace  #"[^A-Za-z0-9-_]" "_"))})

      (input-text {:label "Room Name"
                   :name "name"
                   :required true
                   :placeholder
                   "e.g. 'Fuji' or 'Wellness Centre'"
                   :value name
                   :transform-save trim
                   :type :text
                   :atm atm})

      (input-text {:label "Tower"
                   :name "tower"
                   :required true
                   :placeholder "'North', 'South', or 'Ground Floor'"
                   :value tower
                   :type :text
                   :atm atm})

      (input-text {:label "Floor"
                   :name "floor"
                   :required true
                   :placeholder "e.g. '1' or '8'"
                   :value floor
                   :type :number
                   :atm atm})

      (input-text {:label "Aliases"
                   :name "aliases"
                   :placeholder "e.g. 'Room 101, Winston'"
                   :value aliases
                   :type :text
                   :atm atm
                   :transform-save (fn [s]
                                     (if-not (= "" (trim s))
                                       (str/split (trim s) #",\s*")
                                       []))
                   :transform-display (fn [arr]
                                        (if-not (empty? arr)
                                          (str/join ", " arr)
                                          ""))})

      (input-textarea {:label "Walking Instructions"
                       :required true
                       :name "description"
                       :placeholder "Walking instructions for humans to find the room, e.g. 'Take the door opposite of the kitchen, take a left, pass the ping pong table, then the room is on your left'"
                       :value description
                       :type :textarea
                       :rows 6
                       :style "height: 10em"
                       :atm atm})

      [:div
       [:label "Images"]
       [:div#image-container
        (for [pic  pictures
              :let [img-url (str (env :s3url) pic)]]
          (deletable-image pic img-url :pictures atm))]]

      (image-upload-area {:atm atm :kw :pictures})

      (input-switch {:label-on "This room exists"
                     :label-off "This room doesn't exist anymore"
                     :confirm-off-fn #(.confirm js/window "Are you sure you want to remove this room?")
                     :confirm-on-fn #(.confirm js/window "Are you sure you want to reactivate this room?")
                     :style "margin-top: 30px;"
                     :atm atm} )

      [:button#savebtn.btn.waves-effect.waves-light.btn-large {:style "margin-top: 30px;"} "Save"]]]))


(defn Not-Found []
  [:p.flow-text "This doesn't look like anything to me..."])



(defn hook-autocompleter!
  "Set up autocompleter"
  [rooms]
  (.ready (jquery js/document)
          (fn[]
            (let [data (room-name-to-id rooms)]
              (.autocomplete (jquery "input.autocomplete") "destroy")
              (.autocomplete (jquery "input.autocomplete")
                             (clj->js {:data           (clj->js data)
                                       :matcher        (fn [val list]
                                                         (->> list
                                                              (js->clj)
                                                              (filter #(> (.score % val) 0.105) )
                                                              (clj->js)))
                                       :onAutocomplete (fn [selected-str]
                                                         (let [room-id (get data selected-str)]
                                                           (go-to-room room-id)))
                                       :triggerOnSingleChoice true}))))))




;; -------------------------
;; Load rooms

(defonce rooms (atom {}))

(defn room-change-autouploader
  [_ _ _ new-state]
  (prn "uploading new rooms state to s3")
  (-> (aws/update-rooms& (assoc env :last-updated (js/Date.)) new-state)
      (p/then #(prn "Successfully updated rooms on s3"))
      (p/catch (fn [err] (prn "Error updating room: " err)))))

(defn fetch-rooms
  "Fetch the rooms from S3"
  []
  (-> (aws/download-edn&  (env :bucket-name) (env :rooms-file))
      (p/then (fn [data]
                (remove-watch rooms :auto-upload)
                (reset! rooms data)
                (add-watch rooms :auto-upload room-change-autouploader)
                (prn "FETCHED ROOMS: " (-> data :rooms keys))
                (hook-autocompleter! (data :rooms))
                (act-on-url!)))
      (p/catch (fn [err]
                 (js/alert err)))))



;; -------------------------
;; Routes & Navigation

(defn act-on-url! []
  (secretary/dispatch! (str js/window.location.hash)))

(defn go-to-room [room-id]
  (.setToken history (str "/room/" (name room-id)))
  (act-on-url!))

(def display! (partial set-html! (js/document.getElementById "app")))

(defroute home-path "/" []
  (display! (Home)))

(defroute edit-path "/room/new" []
  (display! (Edit-Room {})))

(defroute room-path "/room/:roomid" [roomid]
  (->> (if-let [room-data (get-room-data (keyword roomid))]
         (Room (merge {:roomid roomid} room-data))
         (Not-Found))
       (display!)))

(defroute room-edit-path "/room/:roomid/edit" [roomid]
  (->> (if-let [room* (get-room-data (keyword roomid))]
         (let [room (merge {:roomid roomid} room*)]
           (Edit-Room room))
         (Not-Found))
       (display!)))

(defroute "*" []
  (display! (Not-Found)))


;; -------------------------
;; Setup

(defonce history (doto (Html5History.)
                   (events/listen
                    EventType/NAVIGATE
                    (fn [event]
                      (secretary/dispatch! (.-token event))))
                   (.setEnabled true)))


(secretary/set-config! :prefix "#")

(aws/configure-aws config/aws-config)


(defn reload! []
  (fetch-rooms))

(defn -main []
  (fetch-rooms))

(comment
  ;; data model
  {:version "date"
   :rooms   {:roomname {:name        "Name"
                        :floor       7
                        :tower       "North"
                        :aliases     ["other" "room" "names"]
                        :pictures    ["picturename"]
                        :description "More info about the name"
                        :moreinfo    ["Links" "or text"]
                        :active      true
                        :last-update "Date"}}}
  )
