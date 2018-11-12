(ns main
  (:require ["aws-sdk" :as AWS]
            [promesa.core :as p]
            [s3atom :refer [s3-atom]]
            [imageupload]
            [hiccups.runtime :as hiccupsrt]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:require-macros [promesa.core]
                   [hiccups.core :as hiccups :refer [html]])
  (:import goog.history.Html5History))

(defonce history (doto (Html5History.)
                   (events/listen
                    EventType/NAVIGATE
                    (fn [event]
                      (secretary/dispatch! (.-token event))))
                   (.setEnabled true)))


(secretary/set-config! :prefix "#")


(def env {:region "us-east-1"
          :rooms-file "rooms.edn"
          :bucket-name "klick-meetingrooms-anonymous"
          :cognito-pool-id "us-east-1:df130540-5cda-4432-aa21-bf1e325f493d"})

(def aws-config {:region (env :region)
                 :credentials (AWS/CognitoIdentityCredentials. #js {:IdentityPoolId (env :cognito-pool-id) })})

(AWS/config.update (clj->js aws-config))

(defonce rooms-s3 (s3-atom aws-config (env :bucket-name) (env :rooms-file)))
(defonce rooms (atom {}))

(defn fetch-rooms
  "Fetch the rooms from S3"
  [cb]
  (-> @rooms-s3
      (p/then (fn [data]
                (reset! rooms data)
                (cb data)))
      (p/catch (fn [err]
                 (js/alert err)))))

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

(defn get-room
  "Pull the room info out of the rooms database"
  [room-id]
  (get-in @rooms [:rooms (keyword room-id)]))

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
          [room-name room-id])))

(defn print-promise
  "For internal use only"
  [p]
  (-> p
          (p/then #(prn 'SUCCESS %))
          (p/catch #(prn 'FAIL %))))


(defn jquery [sel]
  (js/jQuery sel))

(def application
  (js/document.getElementById "app"))

(defn set-html! [el content]
  (aset el "innerHTML" content))


(defn room-header [room]
  [:h4.header {:style "display: inline" } (:name room)
   [:br.hide-on-med-and-up]
   [:h5.grey-text  {:style "display: inline" }
    " ("
    (when (:floor room)
      (str (nthify (:floor room)) " "))
    (when (:tower room)
      (:tower room))
    ")"]])

(defn generate-room [room-id]
  (let [room (get-room room-id)]
    [:div (room-header room)

     (when-let [aliases (:aliases room)]
       (when-not (empty? aliases)
         [:p.grey-text "aka: " (apply str (interpose ", " aliases))]))

     [:div.section
      [:p.flow-text
       (:description room)]]

     (when-let [images (:pictures room)]
       (when-not (empty? images)
         [:div.slider
          [:ul.slides
           (for [img images]
             [:li
              [:img {:src (str "https://s3.amazonaws.com/klick-meetingrooms-anonymous/pics/" img)}]])]]))

     (when-let [more (:moreinfo room)]
       (when-not (empty? more)
         [:ul "More information:"
          (for [info more]
            [:li
             [:a {:href info} info ]])]))

     [:hr]
     [:a.right {:href (str "#/room/" (name room-id) "/edit")}
      "Edit this page"]
     ]))


(defn not-found []
  (set-html! application (html [:p.flow-text "This doesn't look like anything to me..."])))


(defn show-room [el room-id]
  ;; that way this is in the same animation frame and the indicator doesn't flicker
  (js/setTimeout (fn []
                   (if (get-room room-id)
                     (do
                       (set-html! el (html (generate-room room-id)))

                       (-> (jquery ".slider")
                           (.slider (clj->js {"full_width" false
                                              "indicators" (-> (get-room room-id)
                                                               :pictures
                                                               count
                                                               (> 1))})))
                       (js/window.scrollTo 0 0))

                     (not-found)))
                 0))

(defn ^:export set-room-id [room-id]
  (.setToken history (str "/room/" (name room-id))))

(defn act-on-url! []
  (secretary/dispatch! (str js/window.location.hash)))


(defn go-to-room [room-id]
  (set-room-id room-id)
  (act-on-url!)
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
                     :value value}
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



(defroute home-path "/" []
  (set-html! application (html [:span
                                [:p.section.flow-text "Just search for the meeting room by tapping on the search bar above! It supports fuzzy search and as soon as there is only one option left it will automatically load the result!"]
                                [:p.section.flow-text "If you prefer a map with all the rooms take a look at the " [:a {:href "https://genome.klick.com/map/index.html#/"} "Seating Map"]]
                                [:p.section.flow-text "Feel free to link directly to the URLs of the meeting rooms, I will keep the URLs stable."]
                                [:p.section.flow-text "Please also help by contributing to this site by offering corrections, better instructions, additions, comments, and all that. There is a link through which you can email at the bottom of every page."]
                                ])))

(defn edit-form
  [{:keys [roomid name tower floor aliases description]}]
  [:div
   [:form {:id :edit-form}
    (input-text {:label "Room-ID" :name "roomid" :value roomid :readonly (when roomid true) :type :text})
    (input-text {:label "name" :name "name" :value name  :type :text})
    (input-text {:label "Tower" :name "tower" :value tower :type :text})
    (input-text {:label "Floor" :name "floor" :value floor :type :number})
    (input-text {:label "Aliases" :name "aliases" :value aliases :type :text})
    (input-textarea {:label "Description" :name "description" :value description :type :textarea :rows 6 :style "height: 10em"})
    [:div "here we upload and delete pics! Upload just throws it onto s3 and links it in the database."]
    [:div "do we do DDB stuff here or through a lambda?"]
    [:button "Save"]]])


(defroute room-path "/room/:room" [room]
  (show-room application (keyword room)))

(defroute room-edit-path "/room/:roomid/edit" [roomid]
  (let [room (get-room roomid)]
    (.log js/console "room from getroom: " room)
    (set-html! application (html
                            (edit-form (merge {:roomid roomid} room))))))

(defroute edit-path "/room/new" []
  (set-html! application (html
                          (edit-form {:floor 6 :description "test"}))))

;; Catch all
(defroute "*" []
  (not-found))

(defn hook-autocompleter!
  "Set up autocompleter"
  [rooms]
  (.ready (jquery js/document)
          (fn[]
            (let [data (room-name-to-id rooms)]
              (.autocomplete (jquery "input.autocomplete")
                             (clj->js {:data    (clj->js data)
                                       :matcher (fn [val list]
                                                  (->> list
                                                       (js->clj)
                                                       (filter #(> (.score % val) 0.105) )
                                                       (clj->js)))

                                       :onAutocomplete        (fn [selected-str]
                                                                (let [room-id (get data selected-str)]
                                                                  (go-to-room room-id)))
                                       :triggerOnSingleChoice true}))))))

(defn reload! []
  (prn "reload")
  (secretary/dispatch! (str js/window.location.hash)) ;; for reloads
  (.autocomplete (jquery "input.autocomplete") "destroy")
  (fetch-rooms
   (fn []
     (prn "FETCHED ROOMS: " (-> @rooms :rooms keys))
     (hook-autocompleter! (@rooms :rooms)))))

(defn -main []
  (set-html! application "<h1> first load! </h1>")
  (secretary/dispatch! (str js/window.location.hash)) ;; for reloads
  (fetch-rooms
   (fn []
     (prn "FETCHED ROOMS: " (-> @rooms :rooms keys))
     (hook-autocompleter! (@rooms :rooms)))))

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
