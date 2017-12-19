(ns mark.core
  (:require ;;[taoensso.timbre :refer [info]]
            [dommy.core :as d]))


(defn toggle-play [base el]
  (let [img (d/sel1 base :img)]
    (if (d/has-class? img :spin)
      (d/toggle-class! img :spin-pause)
      (d/add-class! img :spin))
    
    (d/toggle-class! el :play)
    (d/toggle-class! el :pause)))


(defn pass-on-bg [base]
  (let [els (d/sel base :.vinyl-wrapper)
        cls (map second (re-seq #"(\bbg[-_][\w]*)" (d/class base)))]
    (doseq [cl cls]
      (d/remove-class! base cl)
      (doseq [el els]
        (d/add-class! el cl)))))


(defn wrap-divs [base]
  (let [els (d/sel base :li)
        spacers (d/sel base :.vinyl-wrapper)]
    (when (empty? spacers)
      (doseq [el els]
        (let [html (.-innerHTML el)]
          (set! (.-innerHTML el)
                (str "<div class=\"vinyl-wrapper\">"
                     html
                     "<div class=\"vinyl-control play\"></div>"
                     "</div>")))))))


(defn add-listen [base]
  (let [el (d/sel1 base :.vinyl-control)]
    (d/listen! el :click #(toggle-play base el))))


(defn ^:export init []
  (letfn [(checker []
            (let [views (d/sel :.flex-viewport)]
              (if (empty? views)
                (js/setTimeout checker 1000)
                (do
                  (doseq [v views]
                    (wrap-divs v)
                    (doseq [div (d/sel v :.vinyl-wrapper)]
                      (add-listen div)))
                  (doall (map pass-on-bg (d/sel :.wpb_gallery)))))))]
    (checker)))


(defn ^:export main []
  (init))
