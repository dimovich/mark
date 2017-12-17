#_(comment

    (defn index []
      (r/with-let [state (r/atom {:play false
                                  :angle 0
                                  :transition-type :linear})
                   set-timer (fn [t]
                               (swap! state assoc :transition-type :linear)
                               (swap! state update :timer
                                      (fn [timer]
                                        (when timer
                                          (js/clearInterval timer))
                                        (js/setInterval (fn [_] (swap! state update :angle direction step)) t))))

                   stop-timer (fn []
                                (swap! state assoc :transition-type :ease-out-cubic)
                                (swap! state update :timer
                                       (fn [timer]
                                         (when timer
                                           (js/clearInterval timer))
                                         nil)))]
        [:div
         [fabric]
         [:div.vinyl-wrap
          [:img.vinyl
           {:src "img/vinyl.png"
            #_(:class (when (:play @state)
                        :spin))
            :style (merge {:transition css-transition
                           :transition-timing-function (:transition-type @state)}
                          (zipmap [:-ms-transform
                                   :-moz-transform
                                   :-webkit-transform
                                   :transform]
                                  (repeat (str "rotate(" (:angle @state) "deg)"))))}]
          [:div.vinyl-control {:class (if (:play @state) :pause :play)
                               :on-click (fn [_]
                                           (swap! state update :play not)
                                           (if (:play @state)
                                             (set-timer transition)
                                             (stop-timer)))}]]]
        (finally
          (some-> @state :timer js/clearInterval)))))







#_(
   (ns mark.core
     (:require [reagent.core :as r]
               [cljsjs.fabric]))


   (def Canvas  window.fabric.StaticCanvas)
   (def IText   window.fabric.IText)
   (def fromURL window.fabric.Image.fromURL)



   (def hz-mult 0.55)
   (def transition 150 ;;(* 1000 (/ (/ 1 hz-mult) 360))
     )                 ;;transition time for each step
   (def step 30)
   (def direction +)
   (def css-transition "0.15s")
   (def transition-type :linear)


   (defn dom-size [dom]
     (when dom
       [(.. dom -clientWidth)
        (.. dom -clientHeight)]))


   (defn vinyl []
     (let [dom        (atom nil)
           unmount-fn (atom nil)
           size (atom nil)
           state (r/atom {:play false
                          :angle 0})]
    
       (r/create-class
        {:component-did-mount
         (fn [_]
           (let [canvas (Canvas. @dom)
                 [x y] @size]
             (doto canvas
               (.setHeight y)
               (.setWidth x))
             (fromURL "img/_vinyl.png"
                      (fn [img]
                        (swap! state assoc :vinyl img)
                        (.set img (clj->js {:width x
                                            :height y
                                            :originX "center"
                                            :originY "center"}))
                        (.add canvas img)
                        (.centerObject canvas img)
                        ;;(.scale img (/ (.getWidth canvas) (.getWidth img) ))
                        ;;(.center img)
                     
                        (.renderAll canvas)
                     
                        (let [easing (fn [t b c d]
                                       (+ (/ (* c t) d) b))
                              animate (fn animate []
                                        (.animate img "angle" 36000 (clj->js {:duration 180100
                                                                              :easing easing
                                                                              :onComplete animate
                                                                              :onChange #(.renderAll canvas)})))]
                          (animate))))))

         :component-did-update
         (fn [this])
      
         :component-will-unmount
         (fn [this])

         :reagent-render
         (fn []
           [:div.vinyl-wrap
            [:div.vinyl {:ref #(reset! size (dom-size %))}
             [:canvas#canv {:ref #(reset! dom %)}]]
         
            [:div.vinyl-control {:class (if (:play @state) :pause :play)
                                 :on-click (fn [_]
                                             (swap! state update :play not))}]])})))


   (defn index []
     [:div
      [vinyl]])

   (comment
     ;;use fabric.js
     ;;use reanimate
     ;;get images from
     )

   (defn ^:export main []
     (r/render [index]
               (.-body js/document)))


   )





#_(
   (defn vinyl []
     (let [dom        (atom nil)
           img-dom    (atom nil)
           size (atom nil)
           state (r/atom {:play false
                          :angle 0})]
    
       (r/create-class
        {:component-did-mount
         (fn [_]
           (let [ctx (m/init @dom "2d")
                 ctx (m/get-context @dom "2d")
                 img (js/Image.)
                 [x y] @size]

             (set! (.-src img) "img/vinyl.png")
             (set! (.-onload img) #(m/draw-image ctx img {:x 0 :y 0 :w x :h y}))
             ))

         :component-did-update
         (fn [this])
      
         :component-will-unmount
         (fn [this])

         :reagent-render
         (fn []
           [:div.vinyl-wrap
            [:canvas.vinyl {:ref #(do (reset! dom %)
                                      (reset! size (dom-size %)))}]
            [:div.vinyl-control {:class (if (:play @state) :pause :play)
                                 :on-click (fn [_]
                                             (swap! state update :play not))}]
            #_[:img {:ref #(reset! img-dom %)
                     :src "img/vinyl.png"}]])}))))





#_(
   (ns mark.core
     (:require 
      ;;[monet.canvas :as m]
      [taoensso.timbre    :refer [info]]
      ;;[reanimated.core :as anim]
      [cljsjs.fabric]
      ;;[quil.core :as q :include-macros true]
      ))


   (defn ^:export main []
     (let [[x y] [400 400]
           img (atom nil)
           angle (atom 0)
           setup-fn (fn []
                      (q/background 0)
                      (q/frame-rate 100)
                      (q/image-mode :center)
                      (reset! img (q/load-image "img/vinyl.svg")))
           draw-fn (fn []
                     (q/background 0)
                     (q/with-translation [200 200]
                       (q/with-rotation [(q/radians @angle)]
                         (q/image @img 0 0 400 400)))
                     (swap! angle (comp #(mod % 360) (partial + 2))))]
    
       (q/defsketch vinyl
         :setup setup-fn
         :draw draw-fn
         :size [x y])))

   )
