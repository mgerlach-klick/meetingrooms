(ns ui
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            ["aws-sdk" :as AWS]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [hiccups.runtime :as hiccupsrt]

            [clojure.string :as str])
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:import goog.History))


(defonce history (doto (History. false)
                    (events/listen
                     EventType/NAVIGATE
                     (fn [event]
                       (secretary/dispatch! (.-token event))))
                    (.setEnabled true)))

(secretary/set-config! :prefix "#")

(defonce rooms (atom {}))

(defn jquery [sel]
  (js/jQuery sel))

(def application
  (js/document.getElementById "app"))


(defn ^:export set-rooms [json]
  (->> (js->clj json :keywordize-keys true)
       (reset! rooms)))

(defn get-room [room-id]
  (get-in @rooms [:rooms room-id]))

(defn nthify [n]
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

(defn set-html! [el content]
  (aset el "innerHTML" content))

(defn room-edit [room-id]
  [:form ])

(defn room-header [room]
  [:h4.header  {:style "display: inline" } (:name room)
   [:br.hide-on-med-and-up]
   [:h5.grey-text  {:style "display: inline" }
    " ("
    (when (:floor room)
      (str
       (nthify (:floor room))
       " "))
    (when (:tower room)
      (:tower room))
    ")"]])

(defn generate-room [room-id]
  (let [room (get-room room-id)]
    [:div

     (room-header room)

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
              [:img {:src (str "/resources/pics/" img)}]])]]))

     (when-let [more (:moreinfo room)]
       (when-not (empty? more)
         [:ul "More information:"
          (for [info more]
            [:li
             [:a {:href info} info ]])]))

     [:hr]
     [:a.right {:href (str "/rooms/" room-id "/edit")}
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
  (.setToken history (str "/room/" room-id)))




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
    (input-text {:label "Room-ID" :name "roomid" :value roomid :readonly (when-not roomid true) :type :text})
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

(defroute edit-path "/edit" []
  (set-html! application (html
                          (edit-form {:floor 6 :description "test"}))))

;; Catch all
(defroute "*" []
  (not-found))

(defn ^:export go-to-fragment []
  (js/console.log "reloading" (str js/window.location.hash))
  (secretary/dispatch! (str js/window.location.hash)) ;; for reloads
  )
