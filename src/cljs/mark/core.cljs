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
        (let [wrapper (d/create-element :div)
              control (d/create-element :div)
              img (d/sel1 el :img)]
          (d/add-class! wrapper "vinyl-wrapper")
          (d/add-class! control "vinyl-control" "play")
          (d/append! wrapper img)
          (d/append! wrapper control)
          (d/append! el wrapper))))))


(defn add-listen [base]
  (let [el (d/sel1 base :.vinyl-control)]
    (d/listen! el :click #(toggle-play base el))))


(defn ^:export init []
  (letfn [(resizer [fun delay count]
            (when (pos? count)
              (js/setTimeout #(do (fun)
                                  (resizer fun delay (dec count)))
                             delay)))
          (checker []
            (let [views (d/sel :.flex-viewport)]
              (if (empty? views)
                (js/setTimeout checker 1000)
                (do
                  (doall (map wrap-divs (d/sel :.flex-viewport)))
                  (doall (map #(do (d/add-class! % :vinyl-spacer)
                                   (doseq [div (d/sel % :.vinyl-wrapper)]
                                     (add-listen div)))
                              (d/sel :.flex-viewport)))
                  (doall (map pass-on-bg (d/sel :.wpb_gallery)))

                  (let [ev (js/Event. "resize")
                        fun #(js/dispatchEvent ev)]
                    (resizer fun 500 3))))))]
    (checker)))


(defn ^:export main []
  (init))
