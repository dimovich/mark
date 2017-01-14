(set-env!
 :source-paths #{"src" "content"}
 :dependencies '[[pandeiro/boot-http "0.7.0"]
                 [perun "0.3.0" :scope "test"]
                 [hiccup "1.0.5"]])

(require '[io.perun :refer :all]
         '[pandeiro.boot-http :refer [serve]])


(deftask dev []
  (comp
   (serve :resource-root "public")
   (watch)
   (markdown)
   (render :renderer 'site.core/page)
   (target :dir #{"target"})))
