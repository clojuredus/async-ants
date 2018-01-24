(ns async-ants.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require
      [reagent.core :as r]
      [cljs.core.async :as a :refer [chan >! <! timeout]]))

(def dimensions [80 75])
(def ants 50)
(def hive-data {:position [30 30]
                :in (chan)})
(def food-points #{[10 10]
                   [70 60]
                   [70 30]
                   [30 70]})

(def pheromone-chan (chan 10))

(defn- walk-rand [[x y]]
    [(mod (+ x (rand-int 3) -1) (dimensions 0))
     (mod (+ y (rand-int 3) -1) (dimensions 1))])

(defn- walk-home [position]
    ;wtf is this code ...
    (->> position
      (map - (:position hive-data)) ; relative vector
      (map #(if (zero? %) 0 (/ % (Math/abs %))))
      (map + position)
      (map #(mod %2 %1) dimensions)))


(defn- on-food? [position]
    (contains? food-points position))

(defn- on-hive? [position]
    (= (:position hive-data) position))


(defn ant [no]
;; --------- MODEL
    (let [state (r/atom {:position (:position hive-data)
                         :food? false})]

;; --------- CONTROLLER
        (go-loop []
            (let [{position :position
                   food? :food?} @state]
              (if food?
                (if (on-hive? position)
                  (do
                    (>! (:in hive-data) no)
                    (swap! state assoc :food? false))
                  (do
                    (>! pheromone-chan [:place position])
                    (swap! state update :position walk-rand)))

                (if (on-food? position)
                  (swap! state assoc :food? true)
                  (swap! state update :position walk-rand))))
            (<! (timeout 100))
            (recur))

;; --------- VIEW

        (fn []
            (let [{[x y] :position
                   food? :food?} @state]
              [:circle.ant
               {:class (when food? "carrying")
                :r 0.3
                :cx x
                :cy y}]))))

(defn food [[x y]]
  [:rect.food
   {:x x
    :y y
    :width 1
    :height 1}])

(defn hive [{[x y :as position] :position
             in :in}]

    (go-loop []
        ; spawn ant
        (js/console.log "Food delivered by: " (<! in))
        (recur))

    (fn []
      [:rect.hive
       {:x x
        :y y
        :width 1
        :height 1}]))

(defn pheromone [[x y] c]
    (let []
      (go-loop [ttl 10]
          (<! (timeout 500))
          (if (not (pos? ttl))
            (>! pheromone-chan [:decayed [x y]])
            (recur (dec ttl))))
      (fn []
        [:rect.pheromone
         {:x x
          :y y
          :width 1
          :height 1}])))

(defn pheromones [c]
    (let [state (r/atom {})]
    ;; controller
      (go-loop []
          (let [new-pheromone (<! c)]
            (case (first new-pheromone)
              :place (swap! state assoc (second new-pheromone) (chan))
              :decayed (swap! state dissoc (second new-pheromone))))
          (recur))

    ;; view
      (fn []
        [:g
         [:title "pheromones"]
         (for [[pher-pos pher] @state]
           ^{:key [:pher pher-pos]}
           [pheromone pher-pos pher])])))

;; -------------------------
;; Views

(defn home-page []
  [:svg.world
   {:viewBox [0 0 (dimensions 0) (dimensions 1)]}
   [pheromones pheromone-chan]
   [hive hive-data]
   [:g
    [:title "ants"]
    (for [ant-no (range ants)]
      ^{:key [:ant ant-no]}
      [ant ant-no])]
   [:g
    [:title "food-points"]
    (for [food-point food-points]
        ^{:key [:food food-point]}
        [food food-point])]])


;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
