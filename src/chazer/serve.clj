#!/usr/bin/env bb

(ns chazer.serve
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [org.httpkit.server :as server]))

(def debug? true)

(def yellow-rgb "rgb(254,224,0)")

(def base-text-style
  {:color       yellow-rgb
   :font-family "sans-serif"
   :text-shadow "6px 15px 4px black"})

(def ^:dynamic ^:private selector-postfix "")


(defn css
  "Garden-like css transformer"
  [g]
  ;(println g )
  (cond
    (or (keyword? g) (string? g))
    (str (name g) selector-postfix " ")

    (map? g)
    (transduce
      (keep (fn [[k v]]
              (when v
                (str "  " (name k) ": " (name v) ";\n"))))
      (completing str #(str % "}\n"))
      "{\n"
      g)

    (sequential? g)
    (transduce (map css) str "" g)))

(def ^:private default-rem-spacings
  "(0 0.25 0.5 1 2 4 8 16)"
  (->> (iterate #(let [n (* % 2)]
                   (if (>= n 1)
                     (int n)
                     n))
                0.25)
       (take 9)
       (cons 0)))

(def ^:private default-pct-spacings
  [10 20 25 30 40 50 60 70 75 80 90 100])

(def ^:private default-font-sizes
  (into
    [0.75 0.875 1 1.25]
    (take 20 (iterate #(+ % 0.75) 1.5))))


(defn ->indexed-spacers
  ([prefix attr unit]
   (->indexed-spacers prefix attr unit default-rem-spacings))
  ([prefix attr unit spacings]
   (let [pr   (name prefix)
         at   (name attr)
         u    (name unit)
         kind (cond
                (#{:font-size :border-radius :line-height} attr) :index-named
                (#{:% :vh :vw} unit) :val-named
                (#{:z-index} attr) :val-named
                (#{:rem} unit) :index-named)]
     (case kind
       :val-named (map
                    (fn [spacing]
                      [(str pr spacing) {at (str spacing u)}])
                    spacings)
       :index-named
       (map-indexed
         (fn [idx spacing]
           [(str pr idx) {at (str spacing u)}])
         spacings)))))
;; ns - not small
;; @media screen and (min-width: 30em)
;; m - medium
;; @media screen and (min-width: 30em) and (max-width: 60em)
;; l - large
;; @media screen and (min-width: 60em)


;z-0: 0
;.z-1: 1
;.z-2: 2
;.z-3: 3
;.z-4: 4
;.z-5: 5
;.z-999: 999
;.z-9999: 9999

(defn css-with-ns-m-l-media [g]
  (let [mq {"-ns" "@media screen and (min-width: 30em)"
            "-m"  "@media screen and (min-width: 30em) and (max-width: 60em)"
            "-l"  "@media screen and (min-width: 60em)"}]
    (str (css g) "\n"
         (transduce
           (map (fn [[postfix media-query]]
                  (binding [selector-postfix postfix]
                    (str media-query " {\n" (css g) "}\n"))))
           str
           mq))
    ))

#_(css (css-with-ns-m-l-media
         [[:.br-pill {:border-radius :9999px}]
          ]))

(css [(str "@media " (css [:.br-pill {:border-radius :9999px}]))])

(def base-css
  (css [[:.flex {:display :flex}
         :.flex-column {:display        :flex
                        :flex-direction :column}
         :.flex-row {:display        :flex
                     :flex-direction :row}]
        [:.flex-wrap {:flex-wrap :wrap}]
        [:.content-center {:align-content :center}]
        [:.items-center {:align-items :center}]
        [:.justify-center {:justify-content :center}]
        [:.no-underline {:text-decoration :none}]
        [:.relative {:position :relative}]
        [:.absolute {:position :absolute}]
        [:.dn {:position :none}]
        [:.br-pill {:border-radius :9999px}]
        [:.br-100 {:border-radius :100%}]
        [:.pointer {:cursor :pointer}]
        (-> (->indexed-spacers :.f :font-size :rem default-font-sizes)
            css-with-ns-m-l-media)
        (-> (->indexed-spacers :.lh :line-height "" [0.8 0.9 1 1.2 1.4 1.5])
            css-with-ns-m-l-media)
        (-> (->indexed-spacers :.w- :width :% default-pct-spacings)
            css-with-ns-m-l-media)
        (-> (->indexed-spacers :.w :width :rem)
            css-with-ns-m-l-media)
        (->indexed-spacers :.h- :height :% default-pct-spacings)
        (->indexed-spacers :.vw- :width :vh default-pct-spacings)
        (->indexed-spacers :.vh- :height :vh default-pct-spacings)
        (->indexed-spacers :.br :border-radius :rem [0 0.125 0.25 0.5 1])
        (->indexed-spacers :.z- :z-index "" [0 1 2 3 4 5 999 9999])
        (->indexed-spacers :.pa :padding :rem)
        (->indexed-spacers :.ma :margin :rem)
        (->indexed-spacers :.pt :padding-top :rem)
        (->indexed-spacers :.pb :padding-bottom :rem)
        (-> (->indexed-spacers :.pl :padding-left :rem)
            css-with-ns-m-l-media)
        (->indexed-spacers :.pr :padding-right :rem)
        (->indexed-spacers :.gap :gap :rem)
        (->indexed-spacers :.row-gap :row-gap :rem)
        (->indexed-spacers :.column-gap :column-gap :rem)
        ]))

(def checkout-link "https://buy.stripe.com/8wM29qgfR0xC4mYeUU")

(def home-html
  (str
    "<!DOCTYPE html>\n"
    (h/html
      [:html
       [:head
        [:title "CHAZER 3000"]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:style {:type "text/css"}
         base-css]]
       [:body.ma0
        {:style {:background "rgb(0,75,255)"}}
        ;[:nav "Chazer"]
        [:div.pt3.flex.w-100.pb4
         [:div.flex.column-gap4.justify-center.flex-wrap.align-center
          [:div.pt4.flex.justify-center.w-70.w8-ns
           {:style {:flex-shrink 0}}
           [:video.w-100
            {:type     "video/mp4"
             :muted    ""
             :autoplay ""
             :loop     ""}
            [:source {:src "/img/chazer-spins.mp4"}]]]
          [:div.flex-column.flex-wrap.items-center
           [:div.pl4-ns.f12-ns.f7
            {:style (assoc base-text-style
                      :font-weight "bold"
                      :font-style "italic")}
            "now only!"]
           [:div.f10.f23-ns.lh1-ns
            {:style (assoc base-text-style
                      :font-weight "bold"
                      ;:line-height "14rem"
                      ;:font-size "16rem"
                      )}
            "$69.69"]]
          [:div.flex.column-gap5.row-gap3.items-center.pt4.flex-wrap.justify-center.w-100
           [:a.f6.f10-ns.w-90.w8-ns
            {:href  checkout-link
             :style (assoc base-text-style
                      ;:font-size "4rem"
                      :font-style "italic"
                      :text-shadow nil
                      :text-align "center"
                      :white-space "nowrap")}
            "BUY NOW!"]
           [:div.w-90.w8-ns
            [:img.w-100
             {;:style {:height "auto"}
              :src "/img/cc-cards.png"}]]
           [:div.w-80.w8-ns
            [:img.w-100
             {:style {:filter "drop-shadow(6px 6px 2px #222)"}
              :src   "/img/chazer-logo.png"}]]]
          [:div.flex.justify-center.w-100.pb4.flex-column.items-center.pt5
           [:div.relative.w-60-ns.w-80-m.w-90
            {:style {:flex-shrink 0}}
            [:div.absolute.w7-ns.w6-m.w5
             {:style {:top "0px" :left "0px" :z-index 1}}
             [:img.br2.w-100
              {:src   "/img/as-seen-on-youtube.jpeg"
               :style {:transform "rotate(-30deg)"}}]]
            [:video.w-100 {:type     "video/mp4"
                           :controls ""}
             [:source {:src "/img/chazer-infomercial.mp4"}]]]]
          [:form {:action checkout-link}
           [:input.f7.pointer {:type "submit" :value "BUY NOW!"}]]]]]])))

;; the home page
(defn home-response []
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    home-html})


(defn with-mime-type [{:as req :keys [uri]} resp]
  (letfn [(assoc-ct [ct]
            (assoc-in resp [:headers "Content-Type"] ct))]
    (if-let [ext (peek (str/split uri #"\."))]
      (assoc-ct
        (case ext
          "ico" "image/x-icon"
          "mp4" "video/mp4"
          "png" "image/png"
          ("jpg" "jpeg") "image/jpeg"))
      resp)))

(defn with-resource [{:as req :keys [uri]}]
  (when-let [resource (io/resource (str "public" uri))]
    (with-mime-type req
                    {:status 200
                     :body   (io/input-stream resource)})))

;; run the server
(defn handler [{:as req :keys [uri body]}]
  (when debug?
    (println "Request:")
    ;(pprint req)
    (pprint uri))
  (let [body     (some-> body slurp java.net.URLDecoder/decode)
        _        (when (and debug? body)
                   (println "Request body:" body))
        response (case uri
                   "/" (home-response)
                   "/favicon.ico" (with-resource
                                    (assoc req
                                      :uri (str "/img" uri)))
                   (with-resource req))]
    (when debug?
      (println "Response:")
      (pprint (dissoc response :body))
      (println))
    response))

#_(def server
  (do
    (when-let [s (resolve 'server)]
      (when (bound? s)
        (s)))
    (println "Server started on port 8080.")
    (server/run-server handler {:port 8080})))

;@(promise)                                                  ;; wait until SIGINT