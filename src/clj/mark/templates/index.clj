(ns mark.templates.index
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [mark.util :refer [ham]]))

(defn index []
  (html5
   {:lang "en"}
   [:head
    [:title "Mark Forge"]
    (include-css "assets/css/ham.css")
    (include-css "assets/css/style.css")]
   [:body
    [:div.content
     [:h1.mark
      "Hello, I am Mark"
      [:br]
      "& I am creating stuff."]]]))




;; http://petercollingridge.appspot.com/svg-optimiser/
;; borders:   https://jsfiddle.net/yyp67pbg/
;; http://www.cssstickyfooter.com/using-sticky-footer-code.html
;; http://alistapart.com/article/holygrail

