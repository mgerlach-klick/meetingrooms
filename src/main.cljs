(ns main
  (:require ["aws-sdk" :as AWS]
            [promesa.core :as p]
            [s3atom :refer [s3-atom]]
            [imageupload]
            [hiccups.runtime :as hiccupsrt]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]

            )
  (:require-macros [promesa.core]
                   [hiccups.core :as hiccups :refer [html]])
  (:import goog.history.Html5History)
  )

(defn hook-browser-navigation! []
  (doto (Html5History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(hook-browser-navigation!)

(secretary/set-config! :prefix "#")

(def rooms (atom {:lastUpdated "2017-01-24T02:33:06.127Z",
 :version "2018-11-11",
 :rooms {:renoir7 {:name "Renoir",
                   :floor 7,
                   :tower "North",
                   :aliases nil,
                   :pictures ["renoir7_1.jpg"],
                   :description "Renoir is in the Klick Health area of the 7th floor, hidden in the corner between PM and strategy.  Entering the 7th floor on the Klick Health side, pass the kitchen to your left, pass left of the closet pillar, and you should be looking at Renoir.",
                   :moreinfo ["https://en.wikipedia.org/wiki/Renoir"],
                   :active true,
                   :last-updated "2018-11-11"},
         :yellow4 {:name "Yellow",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["purpleorangeyellow4_1.jpg"],
                   :description "Yellow is one of a string of tiny meeting rooms along the inside of the 4th floor. Either take the entrance without the corridor that leads to Steve Willer's office and keep going right, or the opposite entrance with the corridor and keep going left. Yellow is the one closest to Steve Willer's office.",
                   :moreinfo [],
                   :active true,
                   :last-updated "2018-11-11"},
         :neilyoung2 {:name "Neil Young",
                      :floor 2,
                      :tower "South",
                      :aliases [],
                      :pictures [],
                      :description "Enter from the elevator into the clear doors to the reception desk. Walk through the doorway to the left of the desk and make a left. Walk directly straight, passed the red support pillar, and you should see 2 meeting rooms on opposites ends of the artistic wall. Neil Young is on the left.",
                      :moreinfo [],
                      :active true,
                      :last-updated "2018-11-11"},
         :cyan4 {:name "Cyan",
                 :floor 4,
                 :tower "North",
                 :aliases [],
                 :pictures [],
                 :description "I couldn't find it!",
                 :moreinfo [],
                 :active true,
                 :last-updated "2018-11-11"},
         :lorioffice2 {:name "Lori Grant's Office",
                       :floor 2,
                       :tower "North",
                       :aliases [],
                       :pictures ["lorigrantoffice2_1.jpg"],
                       :description "Enter the 2nd floor through the non-testing area entrance (it looks more orange!). Lori Grant's office is all the way in the corner to your left, past the big blue 'Decoded Book' wall. Alternatively, enter through the testing area entrance, go around the corner right, and then it'll be tucked in the corner to your left.",
                       :moreinfo ["https://www.klick.com/health/author/lgrant/"],
                       :active true,
                       :last-updated "2018-11-11"},
         :creit0 {:name "Creit",
                  :floor nil,
                  :tower "Lobby",
                  :aliases ["Lobby room"],
                  :pictures ["creit0_1.jpg" "creit0_2.jpg"],
                  :description "The Creit board room is maintained by the building. It's located in the north tower lobby, behind and to the right of the security guys desk. The entrance is, coming from the elevators, to the left.",
                  :moreinfo [],
                  :active true,
                  :last-updated "2018-11-11"},
         :warhol4 {:name "Warhol",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["warhol4_1.jpg"],
                   :description "On the 4th floor, take the entrance without the corridor (the one that leads to Steve Willer's office) and Warhol is directly on your right after going through the door.",
                   :moreinfo ["https://en.wikipedia.org/wiki/Andy_Warhol"],
                   :active true,
                   :last-updated "2018-11-11"},
         :triumph2 {:name "Triumph",
                    :floor 2,
                    :tower "South",
                    :aliases [],
                    :pictures [],
                    :description "Enter from the elevator into the clear doors to the reception desk. Walk through the doorway to the left of the desk and you should see Triumph on the opposite wall, across the hall, just a few feet right from the doorway.",
                    :moreinfo [],
                    :active true,
                    :last-updated "2018-11-11"},
         :denali7 {:name "Denali",
                   :floor 7,
                   :tower "North",
                   :aliases [],
                   :pictures ["denali7_1.jpg"],
                   :description "Denali is on the 7th Floor, North Tower, in the Northeast corner, in the K2 Digital space. It is right next to Lawrence Tepperman's office.",
                   :moreinfo ["https://en.wikipedia.org/wiki/Denali"],
                   :active true,
                   :last-updated "2018-11-11"},
         :purple4 {:name "Purple",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["purpleorangeyellow4_1.jpg"],
                   :description "Purple is one of a string of tiny meeting rooms along the inside of the 4th floor. Either take the entrance without the corridor that leads to Steve Willer's office and keep going right, or the opposite entrance with the corridor and keep going left. Purple is the room closest to the corridor-entrance.",
                   :moreinfo [],
                   :active true,
                   :last-updated "2018-11-11"},
         :vangogh7 {:name "Van Gogh",
                    :floor 7,
                    :tower "North",
                    :aliases nil,
                    :pictures ["vangogh7_1.jpg"],
                    :description "Van Gogh is in the Klick Health area of the 7th floor, opposite of the kitchen, next to Simon Wither's office. ",
                    :moreinfo ["https://en.wikipedia.org/wiki/Vincent_van_Gogh"],
                    :active true,
                    :last-updated "2018-11-11"},
         :wellnesscenter4 {:name "Wellness Center",
                           :floor 4,
                           :tower "North",
                           :aliases [],
                           :pictures ["wellnesscenter4_1.jpg"
                                      "wellnesscenter4_2.jpg"],
                           :description "The Wellness Center is by Leerom Segal's office. On the 4th floor, take the entrance with the corridor in front of it and keep right. I don't know why this is a bookable meeting room.",
                           :moreinfo [],
                           :active true,
                           :last-updated "2018-11-11"},
         :salvadordali3 {:name "Salvador Dali",
                         :floor 3,
                         :tower "North",
                         :aliases [],
                         :pictures ["salvadordali3_1.jpg"],
                         :description "The Salvador Dali is in the Genome and Sensei area. Enter the 3rd Floor North Tower on the reception side, turn right when facing reception, and it will be against the middle of the left wall (past the library).",
                         :moreinfo ["https://en.wikipedia.org/wiki/Salvador_Dalí"],
                         :active true,
                         :last-updated "2018-11-11"},
         :fender3 {:name "Fender",
                   :floor 3,
                   :tower "North",
                   :aliases [],
                   :pictures ["fender3_1.jpg"],
                   :description "Enter through the not-Reception doors. Fender is immediately to your left",
                   :moreinfo ["https://en.wikipedia.org/wiki/Fender_Musical_Instruments_Corporation"],
                   :active true,
                   :last-updated "2018-11-11"},
         :orange4 {:name "Orange",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["purpleorangeyellow4_1.jpg"],
                   :description "Orange is one of a string of tiny meeting rooms along the inside of the 4th floor. Either take the entrance without the corridor that leads to Steve Willer's office and keep going right, or the opposite entrance with the corridor and keep going left. Orange is in the middle, between Purple and Silver.",
                   :moreinfo [],
                   :active true,
                   :last-updated "2018-11-11"},
         :muskoka7 {:name "Muskoka",
                    :floor 7,
                    :tower "North",
                    :aliases [],
                    :pictures ["muskoka7_1.jpg" "muskoka7_2.jpg"],
                    :description "Muskoka is behind the project management area in the Southeast corner of the 7th floor. Turn left on entering the Klick Health area of the 7th floor, pass the kitchen, and it'll be straight ahead and slightly to the right.",
                    :moreinfo ["https://en.wikipedia.org/wiki/Lake_Muskoka"],
                    :active true,
                    :last-updated "2018-11-11"},
         :tragicallyhip2 {:name "The Tragically Hip",
                          :floor 2,
                          :tower "South",
                          :aliases [],
                          :pictures [],
                          :description "Enter from the elevator into the frosted doors opposite of the the reception desk. Make a left and walk past the slight turn and it'll be the first meeting room on your left. If you find the kitchen, you've gone too far.",
                          :moreinfo [],
                          :active true,
                          :last-updated "2018-11-11"},
         :fuji7 {:name "Fuji",
                 :floor 7,
                 :tower "North",
                 :aliases [],
                 :pictures ["fuji7_1.jpg"],
                 :description "Fuji is between the Media/Analytics/Strategy area of KH and K2. Enter the 7th floor on the KH side, keep left, pass the former squash court (and current Media/Analytics/Strategy area) and then Fuji will be on your right.",
                 :moreinfo ["https://en.wikipedia.org/wiki/Mount_Fuji"],
                 :active true,
                 :last-updated "2018-11-11"},
         :red7 {:name "Red",
                :floor 7,
                :tower "North",
                :aliases nil,
                :pictures ["red7_1.jpg"],
                :description "Red is in the Klick Health area of the 7th floor. Upon entering, go right, passing the lab on your right. When entering the editorial area, Red is tucked into the corner on your left, right of Monet.",
                :moreinfo nil,
                :active true,
                :last-updated "2018-11-11"},
         :everest7 {:name "Everest",
                    :floor 7,
                    :tower "North",
                    :aliases [],
                    :pictures ["everest_blackcomb7_1.jpg"
                               "everest_blackcomb7_2.jpg"],
                    :description "Everest is in the K2 area of the 7th floor. Either enter on the KH side, pass the lab on your right, and keep going past Editorial, then you will get to Everest by the foosball table. Or enter though the K2 side, keep left, and keep going until you see Editorial and the foosball table.",
                    :moreinfo ["https://en.wikipedia.org/wiki/Mount_Everest"],
                    :active true,
                    :last-updated "2018-11-11"},
         :pietmondrian3 {:name "Piet Mondrian 3",
                         :floor 3,
                         :tower "North",
                         :aliases ["pm3" "Piet Mondrian Right"],
                         :pictures [],
                         :description "The PM board rooms are right behind the reception desk on the 3rd floor. PM3 is the right-most one",
                         :moreinfo ["https://en.wikipedia.org/wiki/Piet_Mondrian"],
                         :active true,
                         :last-updated "2018-11-11"},
         :smalls4 {:name "Smalls",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["smalls4_1.jpg"],
                   :description "Take the entrance with the corridor in front of it, and then when you enter the Klick area of the floor, Smalls is immediately to your left'",
                   :moreinfo ["https://en.wikipedia.org/wiki/The_Notorious_B.I.G."],
                   :active true,
                   :last-updated "2018-11-11"},
         :leeaaron2 {:name "Lee Aaron",
                     :floor 2,
                     :tower "South",
                     :aliases [],
                     :pictures [],
                     :description "Enter from the elevator into the frosted doors opposite of the the reception desk. Make a left and walk past the slight turn and it'll be the second, smaller meeting room on your left. If you find the kitchen, you've gone too far.",
                     :moreinfo [],
                     :active true,
                     :last-updated "2018-11-11"},
         :pietmondrian2 {:name "Piet Mondrian 2",
                         :floor 3,
                         :tower "North",
                         :aliases ["pm2" "Piet Mondrian Centre"],
                         :pictures [],
                         :description "The PM board rooms are right behind the reception desk on the 3rd floor. PM2 is the centre one",
                         :moreinfo ["https://en.wikipedia.org/wiki/Piet_Mondrian"],
                         :active true,
                         :last-updated "2018-11-11"},
         :blackcomb7 {:name "Black Comb",
                      :floor 7,
                      :tower "North",
                      :aliases nil,
                      :pictures ["everest_blackcomb7_1.jpg"
                                 "everest_blackcomb7_2.jpg"],
                      :description "Black Comb is in the K2 area of the 7th floor. Either enter on the KH side, pass the lab on your right, and keep going past Editorial, then you will get to Black Comb by the foosball table. Or enter though the K2 side, keep left, and keep going until you see Editorial and the foosball table.",
                      :moreinfo ["https://en.wikipedia.org/wiki/Whistler_Blackcomb"],
                      :active true,
                      :last-updated "2018-11-11"},
         :davinci2 {:name "DaVinci",
                    :floor 2,
                    :tower "North",
                    :aliases [],
                    :pictures ["davinci2_1.jpg"],
                    :description "Enter the 2nd floor through the non-testing area entrance. Go left, and DaVinci is right by the blue 'Decoded Book' wall. ",
                    :moreinfo ["https://en.wikipedia.org/wiki/Leonardo_da_Vinci"],
                    :active true,
                    :last-updated "2018-11-11"},
         :jonimitchell2 {:name "Joni Mitchell",
                         :floor 2,
                         :tower "South",
                         :aliases [],
                         :pictures [],
                         :description "Somebody please provide me with instructions :)",
                         :moreinfo ["https://en.wikipedia.org/wiki/Joni_Mitchell"],
                         :active true,
                         :last-updated "2018-11-11"},
         :basecamp7 {:name "Basecamp",
                     :floor 7,
                     :tower "North",
                     :aliases nil,
                     :pictures ["basecamp7_1.jpg"],
                     :description "Basecamp is in the K2 area of the 7th floor. Enter on the K2 side, keep left, and soon it'll be in the corner on your right. ",
                     :moreinfo [],
                     :active true,
                     :last-updated "2018-11-11"},
         :michaelangelo2 {:name "Michaelangelo",
                          :floor 2,
                          :tower "North",
                          :aliases ["Michelangelo"],
                          :pictures ["michaelangelo2_1.jpg"],
                          :description "Michaelangelo is a big meeting room right next to the facilities area on the 2nd floor. Enter through the testing area entrance (the blue looking one!) and it'll be right on your left.",
                          :moreinfo ["https://en.wikipedia.org/wiki/Michelangelo"],
                          :active true,
                          :last-updated "2018-11-11"},
         :biggie4 {:name "Biggie",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["biggie4_1.jpg"],
                   :description "Biggie is next to the Emily Carr by the Wellness centre. Use the non-corridor entrance of the 4th and take a right.",
                   :moreinfo ["https://en.wikipedia.org/wiki/The_Notorious_B.I.G."],
                   :active true,
                   :last-updated "2018-11-11"},
         :pietmondrian1 {:name "Piet Mondrian 1",
                         :floor 3,
                         :tower "North",
                         :aliases ["pm1" "Piet Mondrian Left"],
                         :pictures [],
                         :description "The PM board rooms are right behind the reception desk on the 3rd floor. PM1 is the left-most one",
                         :moreinfo ["https://en.wikipedia.org/wiki/Piet_Mondrian"],
                         :active true,
                         :last-updated "2018-11-11"},
         :blue7 {:name "Blue",
                 :floor 7,
                 :tower "North",
                 :aliases [],
                 :pictures ["blue7_1.jpg"],
                 :description "Blue is in the Klick Health / Editorial area of the 7th floor. Upon entering, go right, passing the lab on your right. When entering the editorial area, Blue is against the wall, by the printer, close to the corner",
                 :moreinfo nil,
                 :active true,
                 :last-updated "2018-11-11"},
         :offsite18 {:description "Suite 1802 on the 18th Floor of the South Tower. You need your Klick security passcard to unlock the door!",
                     :last-updated "2018-11-11",
                     :tower "South",
                     :name "18th Floor South",
                     :moreinfo [],
                     :pictures ["offsite18_1.jpg" "offsite18_2.jpg"],
                     :active true,
                     :aliases ["18th Floor Offsite Meeting Room"],
                     :floor 18,
                     :about "The 18th Floor South Tower is a spacious room devoted to large internal and client meetings or workshops. The room is equipped with a large LED screen, a collaborative glass wall, typical workshop supplies and furniture that can accommodate flexible meeting configurations. And the views are fantastic!"},
         :banksy4 {:name "Banksy",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures ["banksy4_1.jpg"],
                   :description "Banksy is just left of Steve Willer's office. Enter the 4th floor through the entrance that doesn't have a corridor by it, and it's straight ahead to your left.",
                   :moreinfo ["https://en.wikipedia.org/wiki/Banksy"],
                   :active true,
                   :last-updated "2018-11-11"},
         :newparents4 {:description "The New Mother's Room is on the 4th floor, and was previously the QA devices room. Use the 4th floor entrace by the corridor (the side with the toilets) and the room is immediately to your right. Booking notes: If you are a new mother and would like to use the room, please ask your Engagement Advocate for access to the New Mother's Room calendar. Once you have booked a time in the calendar, head to reception to collect a key to the room. Please always lock the door from the inside when you are in the room and when you leave.",
                       :last-updated "2018-11-11",
                       :tower "North",
                       :name "New Mother's Room",
                       :moreinfo [],
                       :pictures [],
                       :active true,
                       :aliases ["New Parent's Room"],
                       :floor 4,
                       :about "This room is exclusively for new mothers who need a private space to express milk. "},
         :alanis2 {:name "Alanis",
                   :floor 2,
                   :tower "South",
                   :aliases [],
                   :pictures [],
                   :description "Enter from the elevator into the frosted doors opposite of the the reception desk. Make a right and you'll see Alanis immediately on your right.",
                   :moreinfo [],
                   :active true,
                   :last-updated "2018-11-11"},
         :monet7 {:name "Monet",
                  :floor 7,
                  :tower "North",
                  :aliases nil,
                  :pictures ["monet7_1.jpg"],
                  :description "Monet is in the Klick Health area of the 7th floor. Upon entering, go right, passing the lab on your right. When entering the editorial area, Monet is immediately to your left.",
                  :moreinfo ["https://en.wikipedia.org/wiki/Claude_Monet"],
                  :active true,
                  :last-updated "2018-11-11"},
         :silver4 {:name "Silver",
                   :floor 4,
                   :tower "North",
                   :aliases [],
                   :pictures [],
                   :description "I couldn't find it!",
                   :moreinfo [],
                   :active true,
                   :last-updated "2018-11-11"},
         :greatbigsea2 {:name "Great Big Sea",
                        :floor 2,
                        :tower "South",
                        :aliases [],
                        :pictures [],
                        :description "Enter from the elevator into the clear doors to the reception desk. Walk through the doorway to the left of the desk and make a right. Great Big Sea is the third of the 3 offices on the right. If you find the kitchen, you've gone too far.",
                        :moreinfo [],
                        :active true,
                        :last-updated "2018-11-11"},
         :nta_lounge2 {:name "North Testing Area, Lounge",
                       :floor 2,
                       :tower "North",
                       :aliases [],
                       :pictures ["nta_boardroom_lounge2_1.jpg"],
                       :description "Take the 2nd floor testing area entrance (the blue looking one!) and the lounge is the first room on the left on your way to the door",
                       :moreinfo [],
                       :active true,
                       :last-updated "2018-11-11"},
         :emilycarr4 {:name "Emily Carr",
                      :floor 4,
                      :tower "North",
                      :aliases [],
                      :pictures ["emilycarr4_1.jpg"],
                      :description "Emily Carr is opposite of the Wellness Centre on the 4th floor. It's very close to the C-level offices. Use either the main entrance with the corridor and take a right, or go past the wash rooms to the entrance closest to Aaron Goldstein's office and go left.",
                      :moreinfo ["https://en.wikipedia.org/wiki/Emily_Carr"],
                      :active true,
                      :last-updated "2018-11-11"},
         :pingpong8 {:description "The ping pong table room has unfortunately been demolished. The table can now be found by the cafe on the 2nd floor.",
                     :last-updated "2018-11-11",
                     :tower "South",
                     :name "Ping Pong table",
                     :old_description "The ping pong table room is on the 8th floor of the South tower. Do not go to through the glass doors, but instead turn around to face the wooden door, and go left. Immediately take the door to your right - if you get to the door with the passcode lock you have gone too far. Once you are through the door follow the signs until you reach the Ping Pong table room. If the door is closed (it shouldn't be!), facilities has the key at their desks (near the Michaelangelo meeting room ;) )",
                     :moreinfo ["https://www.youtube.com/watch?v=8x45dAA3wxM"],
                     :pictures ["pingpong8_1.jpg"
                                "pingpong8_2.jpg"
                                "pingpong8_3.jpg"],
                     :active true,
                     :aliases [],
                     :floor 8},
         :nta_boardroom2 {:name "North Testing Area, Boardroom",
                          :floor 2,
                          :tower "North",
                          :aliases [],
                          :pictures ["nta_boardroom_lounge2_1.jpg"],
                          :description "Take the 2nd floor testing area entrance (the blue looking one!) and the board room is right before the door that actually enters the 2nd floor Klick area.",
                          :moreinfo [],
                          :active true,
                          :last-updated "2018-11-11"},
         :rush2 {:name "Rush",
                 :floor 2,
                 :tower "South",
                 :aliases [],
                 :pictures [],
                 :description "Enter from the elevator into the clear doors to the reception desk. Walk through the doorway to the left of the desk and make a left. Walk directly straight, passed the red support pillar, and you should see 2 meeting rooms on opposites ends of the artistic wall. Rush is on the right.",
                 :moreinfo [],
                 :active true,
                 :last-updated "2018-11-11"},
         :vox3 {:name "Vox",
                :floor 3,
                :tower "North",
                :aliases [],
                :pictures ["vox3_1.jpg"],
                :description "Enter through the not-Reception doors. Vos is immediately to your right",
                :moreinfo ["https://en.wikipedia.org/wiki/Vox_(musical_equipment)"],
                :active true,
                :last-updated "2018-11-11"},
         :whistler7 {:name "Whistler",
                     :floor 7,
                     :tower "North",
                     :aliases [],
                     :pictures ["whistler7_1.jpg"],
                     :description "Whistler is around the corner from Black Comb and Everest in the K2 area of the 7th floor.  Either enter on the KH side, pass the lab on your right, and keep going past Editorial, and right by the foosball table on your left there is Whistler. Alternatively, enter though the K2 side, keep left, and keep going until you see the foosball table, then make a sharp right. If you get to Klick Editorial, you've gone too far. ",
                     :moreinfo ["https://en.wikipedia.org/wiki/Whistler_Blackcomb"],
                     :active true,
                     :last-updated "2018-11-11"}}}))


(def cognito-pool-id "us-east-1:df130540-5cda-4432-aa21-bf1e325f493d")
(def bucket-name "klick-meetingrooms-anonymous")

(def aws-config {:region      "us-east-1"
                 :credentials (AWS/CognitoIdentityCredentials. #js {:IdentityPoolId cognito-pool-id })})

(AWS/config.update (clj->js aws-config))


(defn -file-from-string [s type]
  (js/Blob. [s] #js {:type type}))

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

(defn get-room [room-id]
  (get-in @rooms [:rooms room-id]))

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

(defn print-promise [p]
  (-> p
          (p/then #(prn 'SUCCESS %))
          (p/catch #(prn 'FAIL %))))


(defn jquery [sel]
  (js/jQuery sel))

(def application
  (js/document.getElementById "app"))

(defn set-html! [el content]
  (aset el "innerHTML" content))

;; Catch all
(defroute "/test" []
  (set-html! application (html [:h1 "defroute test"])))

;; Catch all
(defroute "/hello" []
  (set-html! application (html [:h1 "Hello world!"])))


(defn hook-autocompleter!
  [rooms]
  (.ready (jquery js/document)
          (fn[]
            (prn 'document-ready)

            (let [data (room-name-to-id rooms)]
              (.autocomplete (jquery "input.autocomplete")
                             (clj->js {:data    (clj->js data)
                                       :matcher (fn [val list]
                                                  (->> list
                                                       (js->clj)
                                                       (filter #(> (.score % val) 0.105) )
                                                       (clj->js)))

                                       :onAutocomplete        #( prn % '-> data '=> (get data (keyword %)))
                                       :triggerOnSingleChoice true}))))
          )  )

(defn reload! []
  (prn "reload")
  (secretary/dispatch! (str js/window.location.hash)) ;; for reloads
  (.autocomplete (jquery "input.autocomplete") "destroy")
  (hook-autocompleter! (:rooms @rooms))
  )

(defn -main []
  (set-html! application "<h1> first load! </h1>")
  (secretary/dispatch! (str js/window.location.hash)) ;; for reloads
  (hook-autocompleter! (@rooms :rooms))
  )

{:version "date"
 :rooms {:roomname {:name "Name"
                    :floor 7
                    :tower "North"
                    :aliases ["other" "room" "names"]
                    :pictures ["picturename"]
                    :description "More info about the name"
                    :moreinfo ["Links" "or text"]
                    :active true
                    :last-update "Date"}}}
