(ns jvw.core
  (:require [jvw.field :as fld]
            [jvw.entity :as ent]
            [jvw.mirror :as mrr]
            [jvw.portal :as ptl]
            [jvw.tank :as tnk]
            [jvw.mine :as mne]
            [jvw.wall :as wll]
            [jvw.laser :as lsr]))

(def tree (ent/make :tree 1))

(defn explosion [moment-created]
  {:moment-created moment-created :lifetime 4000})

(def field-test
  (-> (fld/make 5 5)
    (fld/introduce [4 4] (wll/make))
    (fld/introduce [2 2] tree)))

(defn laser-segments_r [field location direction segments traversals]
  (if (fld/in? field location)
    (let [traversal [location direction]]
      (if (contains? traversals traversal)
        segments
        (let [entity (fld/entity-at field location)]
          (if entity
            (case (:type entity)
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

(defn has-been? [time past-time ms]
  (<= (+ past-time ms) time))

(defn can-shoot? [field tank]
  (has-been? (fld/time field) (tank :moment-last-shot) 5000))

(defn hit-tank [field attacker-id location]
  (let [time (fld/time field)
        victim-id (fld/entity-id-at field location)]
    (-> field
      (fld/update-entity attacker-id (tnk/add-hit victim-id))
      (fld/update-entity victim-id ent/reduce-energy)
      ((fn [field]
          (if (<= (:energy (fld/entity-id->entity field victim-id)) 0)
            (-> field
              (fld/update-entity victim-id (tnk/set-moment-killed time))
              (fld/clear-tank victim-id)
              (fld/introduce-explosion location (explosion time))
              (fld/update-entity attacker-id (tnk/add-kill victim-id)))
            field))))))

(defn hit-tree [field location]
  (let [tree-id (fld/entity-id-at field location)]
    (-> field
      (fld/update-entity tree-id ent/reduce-energy)
      ((fn [field]
          (if (<= (:energy (fld/entity-id->entity field tree-id)) 0)
            (-> field
              (fld/remove-entity location)
              (fld/introduce-explosion location (explosion (fld/time field))))
            field))))))

(defn check-hit [field tank-id [location segment-type & _]]
  (if (= segment-type :hit)
    (let [time (fld/time field)
          victim (fld/entity-at field location)]
      (case (:type victim)
        :tank
          (hit-tank field tank-id location)
        :tree
          (hit-tree field location)
        ;default
          field))
    field))

(defn shoot [field tank-id]
  (let [tank-original (fld/entity-id->entity field tank-id)]
    (if (can-shoot? field tank-original)
      (let [time (fld/time field)
            position (fld/tank-position field tank-id)
            orientation (tnk/orientation tank-original)
            shot-start-location (fld/adjacent position orientation)
            laser-segments (laser-segments field shot-start-location orientation)]
        [(-> field
          (fld/update-entity tank-id
            (tnk/set-moment-last-shot time))
          (fld/introduce-laser (lsr/make time laser-segments))
          (check-hit tank-id (peek laser-segments))) true])
      [field false])))

(defn movement-destination [field location direction]
  (let [adj (fld/adjacent location direction)
        entity (fld/entity-at field adj)]
    (if (and entity (ptl/is? entity)) (ptl/target entity) adj)))

(defn no-entity-at-location? [field location]
  (not (fld/entity-at field location)))

(defn can-move? [field tank destination]
  (let [time (fld/time field)]
    (and
      (let [entity (fld/entity-at field destination)]
        (or (not entity) (mne/is? entity)))
      (has-been? time (tank :moment-last-shot) 2000)
      (has-been? time (tank :moment-last-move) 2000)
      (fld/in? field destination))))

(defn check-mine [field field-old tank-id]
  (let [position (fld/tank-position field tank-id)
        ead (fld/entity-at field-old position)]
    (if (and ead (mne/is? ead))
      (-> field
        )
      field)))

(defn tank-moved [field tank-id destination direction]
  (-> field
    (fld/update-entity tank-id
      (tnk/set-orientation direction)
      (tnk/set-moment-last-move (fld/time field)))
    (fld/update-position tank-id destination)))

(defn move [field tank-id direction]
  (let [time (fld/time field)
        position (fld/tank-position field tank-id)
        destination (movement-destination field position direction)
        entity (fld/entity-at field destination)]
    (if (can-move? field (fld/entity-id->entity field tank-id) destination)
      (if (and entity (mne/is? entity))
        [(-> field
          (fld/remove-entity destination)
          (tank-moved tank-id destination direction)
          (fld/introduce-explosion destination (explosion time))
          (hit-tank (mne/tank-id entity) destination)) true]
        [(tank-moved field tank-id destination direction) true])
      [field false])))

(defn has-spare-mine? [field tank-id]
  (->> (field :entities)
    (vals)
    (filter mne/is?)
    (filter #(= tank-id (mne/tank-id %)))
    (count)
    (> 2)))

(defn drop-mine [field tank-id direction]
  (if (has-spare-mine? field tank-id)
    (let [position-old (fld/tank-position field tank-id)
          [field-new moved?] (move field tank-id direction)]
      (if moved?
        [(fld/introduce field-new position-old (mne/make tank-id)) true]
        [field false]))
    [field false]))
