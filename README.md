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
- Build jar with `lein uberjar`
- Measure coverage with `lein cloverage`
- Find dead code with `lein yagni`
- Get suggestions for more idiomatic Clojure with `lein kibit`

Some REST calls I use for testing the server:

``` bash
curl -i -X GET  http://localhost:3000/world     -H 'Accept: application/edn'
curl -i -X GET  http://localhost:3000/world     -H 'Accept: application/json'
curl -i -X POST http://localhost:3000/subscribe -H 'Content-Type: application/json' -d '{"name": "Dr.Strange"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "north"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "east"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "south"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "west"}'
curl -i -X POST http://localhost:3000/tank      -H 'Content-Type: application/json' -d '{"tankid": 1, "command": "fire"}'
curl -i -X POST http://localhost:3000/reset     -H 'Content-Type: application/json' -d '{"secret": "do not cheat!"}'
curl -i -X POST http://localhost:3000/validate -H "Content-Type: application/json" -H "Accept: application/json" -d '{"world": "www\nw1w\nwww\n"}'
curl -i -X POST http://localhost:3000/validate -H "Content-Type: application/json" -H "Accept: application/json" -d '{"world": "wwwwwwwwwwww\nw....11....w\nw..........w\nw...tttt...w\nw..t....t..w\nw3.t....t.4w\nw3.t....t.4w\nw..t....t..w\nw...tttt...w\nw..........w\nw....22....w\nwwwwwwwwwwww\n"}'

```

## Credits

- Arjen van Schie for the idea

## Contributors

- Jeroen van Wijgerden, who build a (privately hosted) tankbattle renderer and
  helped during design and development of the tankbattle server.

## Some Ideas

### Features

Design your own world:

- [X] endpoint to validate board-design (and explain what's wrong with it)
- [ ] convert board-design to a game-world via the reset endpoint. Fallback: default world
- [ ] currently validating string with \n ... could that be different? (file?
      vec-of-vec?)

Furthermore:

- [ ] Laser mirrors
- [ ] Teleportation tiles

### Improving user experience

- [ ] Serve intro doc as html
- [ ] Test client (so you can see the requests and responses in the developer
      console?)
- [ ] gists with already prepared tanks in several different languages
- [ ] Swagger docs. (Or describe in the docs that the REST API isn't described
  properly, because Dutch army management 'wants working tanks, instead of
  comprehensive documentation')

### Software Design / Maintainability

- [ ] Align (and bundle?) with latest client
- [ ] CI (build docker container)
- [ ] Remove unused endpoint(s) (start & update)
- [ ] Store all game events (so you can replay the complete game): game was
  reset, tank subscribed, game started, tank command executed, winner detected
- [ ] DOS detection (and prevention?)
- [ ] Monitoring
- [ ] Decent REST API /world/1/tank/2

- [ ] Hexagonal Design. Core contains mapping from positions to gameobjects.
      Boundary converts between normal world representation & position mapping.
- [ ] Return standard data object from core. Something like {:in ...
      :out ... :err ...} and then make conversion to the outside world as thin
      as possible; basically only move data from Response object to external
      representation (like REST server or cmd client, or ...)
- [ ] Some resources (mostly found in the talk pointed to in the first link)
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
