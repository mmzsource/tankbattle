# Introduction to Tank Battle

Congratulations! You are the driver of (one of the rare) Dutch tanks! Well, the
driver ... actually, you are the *programmer* of a Dutch tank. Due to budget
cuts and tank driver layoffs, the Dutch army is now dependent on you to program
their tanks to do what's necessary to survive on the battlefield.

Your very precious tank will be sent to a battleground with ruthless, highly
maneuverable and better equipped enemy tanks that are out to destroy you. Your
tank has to operate autonomously for the duration of the battle. You are allowed
to program your tank up front and improve your program after the battle, but
during a mission, you are not allowed to interfere with the running program.

Since these Dutch tanks where build in 1897, the controls are limited and not
very reliable. You control your tank via a hopelessly old fashioned, badly
designed REST API. With a bit of luck, you can still find some information on
REST API design in archaic parts of the internet archive.

As a proud member of the software concepts department all these terrible
limitations, budget cuts and constraints only motivate you. **Aut viam inveniam
aut faciam** or "I shall either find a way or make one" is our motto!

# Tank Movement

To drive your tanks you can either `[:move-north]`, `[:move-east]`,
`[:move-south]` or `[:move-west]`. The tanks will move one tank-length in that
direction on the battlefield, but the tank engines are known to drop dead
directly after the move and restarting the engine costs 2 seconds. All movement
commands given within this 2 seconds time-frame are simply neglected.

To fire the canon, you simply send the command `[:fire]`. Due to safety reasons a
tank can only `[:fire]` after it hasn't moved for 2 seconds. The reloading mechanism
is terribly slow and you can only fire 5 seconds after your last shot. All fire
commands given within this 5 seconds time-frame are completely neglected.

You are allowed to group commands, like this `[:fire, :move-north]`. You could
also combine commands like this `[:move-north, :move-north, :move-east]`, but a
good reader will understand that the second and third command are useless
(because of the restarting of the engine). The first command *might* be useless.
That depends on the last time a move command was given for this particular tank.

# Battlefield feedback

Luckily, due to a donated radar and some allied battlefield intelligence troops,
you will get pretty accurate battlefield information. After sending a message to
the REST API, you will get a response containing all the information gathered
about the battlefield. For example:

```
{:dimensions  {:width 32 :height 18}
 :game-start  1561786922412
 :game-end    1561787222412
 :last-update 1561786927310
 :tanks      [{:id          1
               :name        "Unicorn"
               :position    [2 2]
               :energy      5
               :orientation :south
               :last-move   1561786983247
               :restarted   1561786985247
               :color       :blue
               :last-shot   1561786922412
               :reloaded    1561786927412
               :hits        7
               :kills       2}]
 :trees      [{:position [3 3] :energy 3}]
 :walls      [{:position [0 0]}
              {:position [1 0]}
              {:position [2 0]}]
 :lasers     [{:start-position [3 1]
               :end-position   [7 1]
			   :direction      :west
			   :start          1561786922412
			   :end            1561786923412}]
 :explosions [{:position [4 4]
               :start    1561786922412
               :end      1561786925412}
              {:position [5 5]
               :start    1561786922412
               :end      1561786925412}]}
```

In addition to getting that information upon every REST API call, this
information is also always available to you via the `/world/` endpoint.

# Who am I?

When you subscribe your tank via the REST API, giving it your teams (hopefully
original) name, you'll get a (random) technical id which should be used to
clearly command your particular tank.

# Rules

- No human interference during the battle
- No Denial Of Service attacks (by accident or on purpose).

Violations of the former rules will lead to judgement by the court-martial,
often leading to immediate termination of the violators employment contract.

- The last tank on the battlefield wins.
- If multiple tanks are still on the battlefield after the game timer has ended,
  the tank with the most kills wins.
- In case of a draw (in terms of number of kills) the tank with the most hits
  wins.
- In case of a draw (in terms of number of hits) the tank with the least amount
  of code wins, counted with the open source tool `cloc` with default settings.
- In case of a draw (in terms of lines of code) it's ... a draw.
