(ns mark.core
  (:require ;;[taoensso.timbre :refer [info]]
            [dommy.core :as d]
            [hipo.core :as h]))


(defn toggle-play [base el]
  (let [img (d/sel1 (d/sel1 base :.flex-active-slide) :img)]
    (d/toggle-class! img :spin)
    (d/toggle-class! el :play)
    (d/toggle-class! el :pause)))


(defn ^:export init []
  (letfn [(checker []
            (let [views (d/sel :.flex-viewport)]
              (if (empty? views)
                (js/setTimeout checker 1000)
                (do
              ;;    (info "views:" views)
                  (doseq [v views]
                    (let [el (h/create [:div.vinyl-control.play])]
                      (d/listen! el :click #(toggle-play v el))
                      (.appendChild v el)))))))]
    (checker)))


(defn ^:export main []
  (init))
