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

Create REST endpoints end-to-end in this order:

- [X] POST create-world (includes working with atom in yada and finding proper time
  library)
- [X] GET world
- [ ] POST create tank subscription
- [ ] POST create start game
- [ ] POST create tank command endpoint

Then in the domain model:

- [ ] fill in update-world 'details'

If time permits:

- [ ] Manually Test (otherwise rely on the automated tests)
- [ ] Test client (so you can see the requests and responses in the developer
      console?)
- [ ] Store all game events (so you can replay the complete game): game was
  reset, tank subscribed, game started, tank command executed, winner detected
- [ ] Swagger docs. (Or describe in the docs that the REST API isn't described
  properly, because Dutch army management 'wants working tanks, instead of
  comprehensive documentation')
- [ ] DOS detection (and prevention?)
- [ ] gists with already prepared tanks in several different languages

### Tank battle design and pseudocode

#### Reset game

- [ ] create new world atom
  - [ ] create a wall around the world
  - [ ] put some trees and walls inside it
  - [ ] create list of player colors to pick from

#### Tank subscription

- [ ] create tank
  - [ ] randomly create id
  - [ ] associate provided name with tank (or pick from default list if none is
    provided (default list is populated with 'anonymous' to begin with))
  - [ ] pick color randomly from list of still available colors and dissoc that
    color from the list
  - [ ] update last-move, restarted, last-shot, reloaded
  - [ ] when color list is empty, lock game for subscription

#### Start game

- [ ] lock game for subscription
- [ ] start game timer

#### Tank command

- [ ] if end time is not reached yet:
  - [ ] execute POSTed commands in order
    - [ ] :fire can only be executed 5 seconds after last shot && 2 seconds
      after last move
    - [ ] :move can only be executed 2 seconds after last :move

    - [ ] In case of :fire
      - [ ] scan the row or column the tank is oriented in to see if something is hit
      - [ ] If other tank is hit:
        - [ ] update other tank (decrement energy)
        - [ ] update firing tank (increment hits)
        - [ ] if tank has 0 energy left
          - [ ] replace other tank for an explosion
          - [ ] update firing tank (increment kills)
      - [ ] If tree is hit:
        - [ ] update tree (decrement energy)
        - [ ] if tree has 0 energy left
          - [ ] replace tree for an explosion

    - [ ] in case of :move
      - [ ] if move-to cell is empty
        - [ ] move
        - [ ] update last-move and restarted times

    - [ ] ?remove expired explosions?
    - [ ] calculate winner

- [ ] otherwise
  - [ ] calculate winner
