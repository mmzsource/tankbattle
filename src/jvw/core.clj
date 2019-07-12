(ns jvw.core
  (:require [jvw.field :as fld]
            [jvw.entity :as ent]
            [jvw.mirror :as mrr]
            [jvw.portal :as ptl]
            [jvw.tank :as tnk]
            [jvw.mine :as mne]
            [jvw.wall :as wll]
            [jvw.laser :as lsr]))

(def tank (tnk/make :west))

(def field1
  (-> (fld/make 12 12)
    (assoc-in [:entities [2 3]] tank)
    (assoc-in [:tank-positions (ent/id tank)] [2 3])
    (assoc-in [:entities [0 3]] (mrr/make false))
    (assoc-in [:entities [0 0]] (ptl/make [2 2]))
    (assoc-in [:entities [2 0]] (wll/make))
    (assoc-in [:entities [0 2]] (mne/make (ent/id tank)))))

(def field2
  (-> (fld/make 3 4)
    (assoc-in [:entities [2 3]] tank)
    (assoc-in [:entities [0 3]] (ptl/make [1 0]))
    (assoc-in [:entities [0 0]] (mrr/make true))
    (assoc-in [:entities [0 2]] (mrr/make false))
    (assoc-in [:entities [2 2]] (mrr/make true))
    (assoc-in [:entities [2 0]] (mrr/make false))))

(defn laser-segments_r [field location direction segments traversals]
  (if (fld/in? field location)
    (let [traversal [location direction]]
      (if (contains? traversals traversal)
        segments
        (let [entity (fld/entity-at field location)]
          (if entity
            (case (ent/type entity)
              :mirror
                (let [new-direction (mrr/reflection entity direction)
                      new-location (fld/adjacent location new-direction)
                      segment [location :mirror direction new-direction]]
                  (recur field new-location new-direction (conj segments segment) (conj traversals traversal)))
              :portal
                (let [new-location (ptl/target entity)
                      segment1 [location :portal-entry direction]
                      segment2 [new-location :portal-exit direction]]
                  (recur field new-location direction (conj segments segment1 segment2) (conj traversals traversal)))
              :mine
                (recur field (fld/adjacent location direction) direction segments (conj traversals traversal))
              ;default
                (conj segments [location :hit direction]))
            (recur
              field
              (fld/adjacent location direction)
              direction
              (conj segments [location :straight direction])
              (conj traversals traversal))))))
    segments))

(defn laser-segments [field location direction]
  (laser-segments_r field location direction [[location :start direction]] #{}))

(defn fire [field tank]
  ; set tank fired time
  ; do something with hit entity
  ; create laser
  )

(defn can-move? [field tank position new-position]
  true)

(defn move-tank [field tank direction]
  (let [position (fld/tank-position field tank)
        new-position (fld/adjacent position direction)]
      (if (can-move? field tank position new-position)
        (-> field
          (assoc-in [:tank-positions ]))
        field)))
