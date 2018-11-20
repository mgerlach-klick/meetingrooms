(ns ui
  (:require [hiccups.runtime :as hiccupsrt])
  (:require-macros
                   [hiccups.core :as hiccups :refer [html]]))


(defn jquery [sel]
  (js/jQuery sel))

(def +javascripts+ (atom []))

(defn execute-js!
  "Execute a bunch of javascripts"
  [javascripts]
  (doseq [script javascripts]
    (cond
      (string? script) (.eval js/window script)
      (ifn? script)    (script)
      :else            (throw "Not supported"))))

(defn add-js
  [script]
  (swap! +javascripts+ conj script))

(defn set-html!
  ([el content]
   (aset el "innerHTML" (html content))
   (execute-js! @+javascripts+)
   (reset! +javascripts+ []))

  ([el content javascript]
   (set-html! el content)
   (.eval js/window javascript)))

(defn by-id [^String id]
  (let [idstr (if (.startsWith (name id) "#")
                (name id)
                (str "#" (name id)))]
    (jquery (str "#" (name id)))))


(defn on-event [el event fn]
  (.on el (name event) fn))

(defn event-val [event]
  (.. event -target -value))

(defn on-click [el fn]
  (on-event el "click" fn))

(defn append-hiccup [el hcp]
  (.append el (html hcp))
  (execute-js! @+javascripts+)
  (reset! +javascripts+ []))

(defn alert [& params ]
  (js/swal (->> (apply hash-map params)
                (merge {:type "success"})
                (clj->js))))

(defn icon
  ([icon-name size orientation]
   [:i {:class (str "material-icons " (name size) " " (name orientati))} (name icon-name)])
  ([icon-name size]
   [:i {:class (str "material-icons " (name size))} (name icon-name)])
  ([icon-name]
   [:i.material-icons (name icon-name)]))

(defn scroll-up []
  (js/window.scrollTo 0 0))
