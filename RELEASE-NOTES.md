# v0.2

Feature:

- A tank is now allowed to fire immediately after a move
  - In earlier tankbattle versions a tank could only fire after it hadn't moved
    for 2 seconds AND the reload timer had ended
  - As of now, a tank can fire directly after a move if-and-only-if the reload
    timer has ended

No API changes were made to remove this tank rule. But be aware: tank programs
that leverage this safety rule removal will have an advantage over tanks
assuming the safety rule still exists.

# v0.1

Features:

- Design your own tankbattle boards! (see README)
- endless subscription

Breaking API changes:

- `/subscribe` endpoint returns newly subscribed tank (complete map, including id)
- `/reset` endpoint returns newly created world

Non Breaking API changes:

- all gameobjects now have uuids
- `/reset` endpoint can now be used to specify your own board (see README)
