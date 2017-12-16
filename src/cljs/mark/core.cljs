(ns mark.core
  (:require [reagent.core :as r]))

(def hz-mult 0.55)
(def transition (/ (/ 1 hz-mult) 36))
(def step 10)
(def direction +)



(defn index []
  (let [state (r/atom {:control :play
                       :angle 0})]
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

