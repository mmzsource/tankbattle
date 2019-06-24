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
