(set-env!
 :source-paths #{"src/clj" "src/cljs" "content"}
 :resource-paths #{"html"}
 :dependencies '[[org.clojure/clojurescript "1.9.293"]
                 [adzerk/boot-cljs "1.7.228-2"]
                 [pandeiro/boot-http "0.7.6"]
                 [adzerk/boot-reload "0.5.0"]
                 [adzerk/boot-cljs-repl "0.3.3"]
                 [com.cemerick/piggieback "0.2.1" :scope "test"]
                 [weasel "0.7.0" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 [compojure "1.5.2"]
                 [hiccup "1.0.5"]
                 [perun "0.3.0" :scope "test"]
                 [hiccup "1.0.5"]
                 [prismatic/dommy "1.1.0"]])

(require '[io.perun :refer [markdown render]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         'boot.repl)

(swap! boot.repl/*default-dependencies*
       concat '[[cider/cider-nrepl "0.15.0-SNAPSHOT"]])

(swap! boot.repl/*default-middleware*
       conj 'cider.nrepl/cider-middleware)


(deftask dev []
  (comp
   (serve :resource-root "target"
          :handler 'mark.core/app)
   (watch)
   (reload)
   ;; (markdown)
   ;; (render :renderer 'site.core/page)
   (cljs-repl)
   (cljs)
   (target :dir #{"target"})))


(deftask release []
  (comp
   (cljs :compiler-options {:optimizations :advanced})
   (target :dir #{"target"})))
