(ns mark.util)


(defn ham [& args]
  [:div
   [:div#hambtn.hambtn.clickable.hamburger {:class "hamburger--collapse"}
    [:div.hamburger-box
     [:div.hamburger-inner]]]
   (into [:div#sidenav.sidenav.disabled]
         (map #(let [[n h] %]
                 [:div [:a {:href h :data-scroll true} n]])
              (partition 2 args)))])
