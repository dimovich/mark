(ns mark.core 
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found files resources]]
            [compojure.handler :refer [site]]
            [mark.templates.index :refer [index]]))



(defroutes handler
  (GET "/index.html" [] (index))
  (files "/" {:root "target"})     ;; to serve static resources
  (resources "/" {:root "target"}) ;; to serve anything else
  (not-found "Page Not Found"))    ;; page not found


(def app
  (-> handler
      (site)))

