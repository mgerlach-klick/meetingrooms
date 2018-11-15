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
