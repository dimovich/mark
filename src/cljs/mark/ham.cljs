(ns mark.ham
  (:require [dommy.core :as d :refer-macros [sel1]]))

(def ham-visible (atom false))

(defn toggler [nav btn]
  (fn [e]
    (.stopPropagation e)
    (d/toggle-class! nav "disabled")
    (d/toggle-class! btn "is-active")
    (swap! ham-visible not)))

(defn setup-ham []
  (let [nav (sel1 :#sidenav)
        btn (sel1 :#hambtn)
        toggle-ham! (toggler nav btn)
        close-ham! (fn [e]
                     (when @ham-visible
                       (toggle-ham! e)))]
    
    (d/listen! btn :click toggle-ham!)
    (d/listen! (sel1 :body) :click close-ham!)
    (d/listen! (sel1 :body) :keyup (fn [e]
                                         (if (= (.-keyCode e) 27)
                                           (close-ham! e))))))
