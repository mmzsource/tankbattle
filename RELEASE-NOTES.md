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
