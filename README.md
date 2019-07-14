# Tank Battle

This shooter is meant as a fun game for team building and for learning to work
with a REST API.

- Hand out the quick reference cards to a party (group or person)
- Let the party experience the REST API via the simulation environment
  (basically send a world-state AND a command and get back the new world-state)
- Give the parties limited time to program the tanks to win battles
- When two or more parties subscribe to the game, the tank battle can start
- Hands of the keyboard during the battle!

## Installation

The running environment minimally contains:

- 1 server
- 1 (big) screen with a game-master client rendering one playgound
- 2 REST API clients
- a network connecting the server and clients

## Usage

- Startup the server
- Communicate the server address

</br>

- `<CODE>`
- BATTLE!
- Repeat

## Development

- Tested with leiningen 2.9.1 on Java 1.8.0_05. (known problems loading
  javax.bind.xml as of java 9 ... didn't bother to fix these java related
  problems yet). So make sure your JAVA_HOME points to a java 8 distribution.

- Test with `lein test`
- Run server with `lein run` (kill it with Ctrl-C)
- Build jar with `lein uberjar` and then run with `java -jar target/uberjar/tankbattle-0.1.0-SNAPSHOT-standalone.jar`
- Measure coverage with `lein cloverage`
- Find dead code with `lein yagni`
- Get suggestions for more idiomatic Clojure with `lein kibit`

## Docker

- Build a docker image with `docker build -t tankbattle:v01 .`
- Image should be available in `docker images` list
- Run with `docker run -d -p 3000:3000 --name tankbattle tankbattle:v01`

## CURL for testing

Some REST calls I use for e2e testing of the server:

``` bash
curl -i -X GET  http://localhost:3000/world     -H 'Accept: application/edn'
curl -i -X GET  http://localhost:3000/world     -H 'Accept: application/json'
curl -i -X POST http://localhost:3000/subscribe -H 'Content-Type: application/json' -d '{"name": "Dr.Strange"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "north"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "east"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "south"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "west"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "fire"}'

# default world
curl -i -X POST http://localhost:3000/reset     -H 'Content-Type: application/json' -d '{"secret": "do not cheat!"}'

# default world
curl -i -X POST http://localhost:3000/reset     -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"secret": "do not cheat!", "board": [["wwwwwwwwwwww"], ["w....1.....w"], ["w..........w"], ["w...tttt...w"], ["w..t....t..w"], ["w..t....t.4w"], ["w3.t....t..w"], ["w..t....t..w"], ["w...tttt...w"], ["w..........w"], ["w.....2....w"], ["wwwwwwwwwwww"]]}'

# easy for E2E test on a renderer
curl -i -X POST http://localhost:3000/reset     -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"secret": "do not cheat!", "board": [[".wwwww."], ["w1.t.2w"], [".wwwww."]]}'

# lambda world
curl -i -X POST http://localhost:3000/reset     -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"secret": "do not cheat!", "board": [["wwwwwww.............."],["w1.....w............."],["wwww....w............"],["....w....w..........."],[".....w....w.........."],["......w....w........."],[".......wttttw........"],["......wttttttw......."],[".....wttttttttw......"],["....w....ww....w....."],["...w....w..w....w...."],["..w....w....w....wwww"],[".w2...w......w.....3w"],["wwwwww........wwwwwww"]]}'

curl -i -X POST http://localhost:3000/validate -H "Content-Type: application/json" -H "Accept: application/json" -d '{"world": [["www"],["w1w"],["www"]]}'
curl -i -X POST http://localhost:3000/validate -H "Content-Type: application/json" -H "Accept: application/json" -d '{"world": [["wwwwwwwwwwww"], ["w....1.....w"], ["w..........w"], ["w...tttt...w"], ["w..t....t..w"], ["w3.t....t..w"], ["w..t....t.4w"], ["w..t....t..w"], ["w...tttt...w"], ["w..........w"], ["w.....2....w"], ["wwwwwwwwwwww"]]}'
```

## Credits

- Arjen van Schie for the tankbattle idea he presented at a Devnology meetup
  years ago.

## Contributors

- Jeroen van Wijgerden, who built a (privately hosted) tankbattle renderer and
  helped during design and development of the tankbattle server.
- Gertjan Maas who created the docker file and helped deploying the server
- Jasper Stam who built an awesome [tankbattle
  renderer](https://github.com/stam/tankbattle-renderer) (and kept asking for
  new cool features)

## Some Ideas

### Features

Design your own world:

- [X] endpoint to validate board-design (and explain what's wrong with it)
- [ ] convert board-design to a game-world via the reset endpoint. Fallback: default world

Furthermore:

- [ ] Laser mirrors -> smallest possible change (?): simply have one more target
      type on the board that can be shot. Handle a laser shot exactly the same
      as it's handled now (so {:start-position [x1 y1] :end-position [x2 y2]
      :direction :south}) where start-position is still a tank and end-position
      is now a laser-mirror. Then, after updating all tank shots, determine
      which laser-mirrors where hit, and fire another laser from that
      laser-mirror in the correct direction, using the same logic used when
      firing a tank. So for this laser, the start-position is the laser-mirror
      position and the end-position depends on the placement of the mirror.
- [ ] Teleportation tiles -> smallest possible change (?): keep a bidirectional
      map with links between teleportation tiles, e.g. {[0 3] [12 6] [12 6] [0
      3]}. Make sure a tank is allowed to move on a teleportation tile. When the
      tank move is done (nothing needs to change there), use the new tank
      position and the bidirectional map to find out if the tank should be
      tele-ported and if so to which position it should be teleported.

### Improving user experience

- [ ] Serve intro doc as html
- [ ] Provide a test client (so you can see the requests and responses in the
      developer console of a browser)
- [ ] gists with already prepared tanks in several different languages
- [ ] Swagger docs. (Or describe in the docs that the REST API isn't described
  properly, because Dutch army management 'wants working tanks, instead of
  comprehensive documentation')

### Software Design / Maintainability

- [ ] Replace yada. Its coersion magic does not fit well with how my brain works
      and I haven't found enough documentation or examples to finally see the
      light.
- [ ] Align (and bundle?) with latest client
- [ ] CI (build docker container)
- [ ] Remove unused endpoint(s) (start & update)
- [ ] Store all game events (so you can replay the complete game): game was
  reset, tank subscribed, game started, tank command executed, winner detected
- [ ] DOS detection (and prevention?)
- [ ] Logging
- [ ] Monitoring
- [ ] Decent REST API? /world/1/tank/2
- [ ] Hexagonal Design. Core contains mapping from positions to gameobjects.
      Boundary converts between normal world representation & position mapping.
- [ ] Return standard data object from core. Something like {:out ... :err ...}
      and then make conversion to the outside world as thin as possible;
      basically only move data from Response object to external representation
      (like REST server or cmd client, or web socket, or ...)

### Data driven design ideas

- Effect ive Clojure about data oriented Clojure -
  https://www.youtube.com/watch?v=IZlt6hH8YiA
- UltraTestable Coding Style -
  https://blog.jessitron.com/2015/06/06/ultratestable-coding-style/
- Mike Acton "Data-Oriented Design and C++" -
  https://www.youtube.com/watch?v=rX0ItVEVjHc
- Carmack on inlined code -
  http://number-none.com/blow/blog/programming/2014/09/26/carmack-on-inlined-code.html
- Boundaries - https://www.destroyallsoftware.com/talks/boundaries
- Functional Core, Imperative Shell
  https://www.destroyallsoftware.com/screencasts/catalog/functional-core-imperative-shell
- Core.Async in Use - Timothy Baldridge - https://www.youtube.com/watch?v=096pIlA3GDo
- Transparency through data by James Reeves - https://www.youtube.com/watch?v=zznwKCifC1A
- ClojureScript Concurrency Revisited – Paulus Esterhazy - https://www.youtube.com/watch?v=rV1gTJ2wsZg
- Zach Tellman - ABC - Always Be Composing - https://www.youtube.com/watch?v=3oQTSP4FngY

And then of course:

- Solving problems the Clojure way - https://www.youtube.com/watch?v=vK1DazRK_a0
- The only abstractions that really matter are those that give exponential
  benefits by changing the game. The danger of small-ball abstractions is they
  obscure the big picture of what your program is doing and prevent the big
  wins. - @kovasb
