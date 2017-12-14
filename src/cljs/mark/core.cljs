(ns mark.core
  (:require [reagent.core :as r]))

(defn index []
  (let [state (r/atom {:control :play})]
    (fn []
      [:div
       [:div.vinyl-wrap
        [:img.vinyl
         {:src "img/vinyl.png"
          :class (when (= :pause (:control @state))
                   :spin)}]
        [:div.vinyl-control {:class (:control @state)
                             :on-click (fn [_]
                                         (swap! state update :control #(if (= % :play) :pause :play)))}]]])))

(defn ^:export main []
  (r/render [index]
            (.-body js/document)))

