(ns main
  (:require ["aws-sdk" :as AWS]
            [promesa.core :as p]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [components :refer [input-text input-textarea deletable-image image-upload-area]]
            [ui :refer [jquery execute-js! add-js set-html! ]]
            [clojure.string :as str]
            [config :refer [env] :as config]
            [aws])
  (:require-macros [promesa.core])
  (:import goog.history.Html5History))

(defonce history (doto (Html5History.)
                   (events/listen
                    EventType/NAVIGATE
                    (fn [event]
                      (secretary/dispatch! (.-token event))))
                   (.setEnabled true)))


(secretary/set-config! :prefix "#")

(aws/configure-aws config/aws-config)

(defonce rooms (atom {}))

(def application
  (js/document.getElementById "app"))

(defn room-change-autouploader
  [_ _ _ new-state]
  (prn "uploading new rooms state to s3")
  (-> (aws/update-rooms& (assoc env :last-updated (js/Date.)) new-state)
      (p/then #(prn "Successfully updated rooms on s3"))
      (p/catch (fn [err] (prn "Error updating room: " err)))))

(defn fetch-rooms
  "Fetch the rooms from S3"
  [cb]
  (-> (aws/download-edn&  (env :bucket-name) (env :rooms-file))
      (p/then (fn [data]
                (remove-watch rooms :auto-upload)
                (reset! rooms data)
                (add-watch rooms :auto-upload room-change-autouploader)
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

(defn print-oromise
  "For internal use only"
  [p]
  (-> p
      (p/then #(prn 'SUCCESS %))
      (p/catch #(prn 'FAIL %))))



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

    (add-js (fn []
              (-> (jquery ".slider")
                  (.slider (clj->js {"full_width" false
                                     "indicators" (-> (get-room room-id)
                                                      :pictures
                                                      count
                                                      (> 1))})))
              (js/window.scrollTo 0 0)))

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
              [:img {:src (str (env :s3url) img)}]])]]))

     (when-let [more (:moreinfo room)]
       (when-not (empty? more)
         [:ul "More information:"
          (for [info more]
            [:li
             [:a {:href info} info ]])]))

     [:hr]
     [:a.right {:href (str "#/room/" (name room-id) "/edit")}
      "Edit this page"]]))


(defn not-found []
  (set-html! application [:p.flow-text "This doesn't look like anything to me..."]))


(defn show-room [el room-id]
  ;; that way this is in the same animation frame and the indicator doesn't flicker
  (if (get-room room-id)
    (set-html! el (generate-room room-id))
    (not-found)))

(defn ^:export set-room-id [room-id]
  (.setToken history (str "/room/" (name room-id))))

(defn act-on-url! []
  (secretary/dispatch! (str js/window.location.hash)))


(defn go-to-room [room-id]
  (set-room-id room-id)
  (act-on-url!)
  )




(defroute home-path "/" []
  (set-html! application [:span
                          [:p.section.flow-text "Just search for the meeting room by tapping on the search bar above! It supports fuzzy search and as soon as there is only one option left it will automatically load the result!"]
                          [:p.section.flow-text "If you prefer a map with all the rooms take a look at the " [:a {:href "https://genome.klick.com/map/index.html#/"} "Seating Map"]]
                          [:p.section.flow-text "Feel free to link directly to the URLs of the meeting rooms, I will keep the URLs stable."]
                          [:p.section.flow-text "Please also help by contributing to this site by offering corrections, better instructions, additions, comments, and all that. There is a link through which you can email at the bottom of every page."]
                          ]))

(defn edit-form
  [{:keys [roomid name tower floor aliases description pictures] :as room}]
  (reset! components/my-atom room)
  (let [atm components/my-atom]
    (add-js (fn []
              (ui/on-click
               (ui/by-id :savebtn)
               (fn []
                 (prn "saving room")
                 (swap! rooms assoc roomid @atm )))))

    [:div
     [:form {:id :edit-form}
      (input-text {:label "Room-ID" :name "roomid" :value roomid :readonly (when roomid true) :type :text :atm atm})
      (input-text {:label "Room Name" :name "name" :value name :type :text :atm atm})
      (input-text {:label "Tower" :name "tower" :value tower :type :text :atm atm})
      (input-text {:label "Floor" :name "floor" :value floor :type :number :atm atm})
      (input-text {:label "Aliases" :name "aliases" :value (str/join ", " aliases) :type :text :atm atm})
      (input-textarea {:label "Description" :name "description" :value description :type :textarea :rows 6 :style "height: 10em" :atm atm})

      [:div
       [:label "Images"]
       [:div#image-container
        (for [pic  pictures
              :let [img-url (str (env :s3url) pic)]]
          (deletable-image pic img-url :pictures atm))]]

      (image-upload-area {:atm atm :kw :pictures})

      [:div "here we upload and delete pics! Upload just throws it onto s3 and links it in the database."]
      [:div "do we do DDB stuff here or through a lambda?"]
      [:button#savebtn "Save"]]]))


(defroute room-path "/room/:room" [room]
  (show-room application (keyword room)))

(defroute room-edit-path "/room/:roomid/edit" [roomid]
  (let [room* (get-room roomid)]
    (prn "room from getroom: " room*)
    (let [room (merge {:roomid roomid} room*)]
      (set-html! application (edit-form room)))))

(defroute edit-path "/room/new" []
  (set-html! application (edit-form {:floor 6 :description "test"})))

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
     (hook-autocompleter! (@rooms :rooms))
     (act-on-url!))))

(defn -main []
  (fetch-rooms
   (fn []
     (prn "FETCHED ROOMS: " (-> @rooms :rooms keys))
     (hook-autocompleter! (@rooms :rooms))
     (act-on-url!))))

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
