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

## Examples

TODO: Add some code examples of moving the tank in curl, postman, javascript,
clojure, etc.

## Development

- Tested with leiningen 2.9.1 on Java 1.8.0_05. (known problems loading
  javax.bind.xml as of java 9 ... didn't bother to fix these java related
  problems yet).

- Test with `lein test`
- Run server with `lein run` (kill it with Ctrl-C)
- Build jar with `lein uberjar`

## Credits

- Arjen van Schie for the idea

## Contributors

- Jeroen van Wijgerden, who's building a [rendering
  client](https://github.com/jeroenvanw/tank-battle-rendering) and helped
  during design and development of the tankbattle server.

## TODO

### Plan

- [ ] cleanup expired explosions & lasers
- [ ] calculate winner
- [ ] stop when timer is finished
- [ ] Manually Test (otherwise rely on the automated tests)

If time permits:

- [ ] Test client (so you can see the requests and responses in the developer
      console?)
- [ ] Store all game events (so you can replay the complete game): game was
  reset, tank subscribed, game started, tank command executed, winner detected
- [ ] Swagger docs. (Or describe in the docs that the REST API isn't described
  properly, because Dutch army management 'wants working tanks, instead of
  comprehensive documentation')
- [ ] DOS detection (and prevention?)
- [ ] gists with already prepared tanks in several different languages

### Some Ideas

- [ ] Hexagonal Design. Core contains mapping from positions to gameobjects.
      Boundary converts between normal world representation & position mapping.
- [ ] Return standard data object from core. Something like {:response ...
      :error-message ...} and then make conversion to the outside world as thin
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
