# GTA7

TRAPX doctrine, live on a real Minecraft server. GTA7 is a plugin running inside
**EINHORN_SURVIVAL** (`mc.okemily.com`) — there's no separate server or client to install.

## Connecting

**Java Edition**: `mc.okemily.com` — plain address, no port needed.

**Bedrock Edition** (phone/console/Windows): also `mc.okemily.com`, but type the port
explicitly: **`19133`**. Bedrock's "Add Server" screen has a separate port field for this.

Access is open — real account required (no cracked/offline clients), no whitelist. See
`EINHORN_SURVIVAL/README.md` for the full connection details and server status commands.

## Commands

| Command | Who | What it does |
|---|---|---|
| `/flow` | anyone | Shows your Flow balance and how many Field Offices you currently hold. |
| `/faction` | anyone | Shows your current faction and reputation. |
| `/faction join <FREQUENCY\|BLOC\|PROCUREMENT>` | anyone | Joins (or switches to) one of the three player-alignable factions. |
| `/gta7tv` | anyone | Places a real, floating broadcast screen at your location showing a rolling feed of recent city activity — claims, flips, contests, Enforcement, Party Store closures, Krankenvagen runs. No resource pack or client mod needed. |
| `/sudoku` | anyone | Self-KO → immediate respawn. Use this if you're stuck in terrain (a hole, a void gap, wedged geometry) with no other way out. Does **not** count as an Enforcement kill, so it never sends you to Custody Lock. |
| `/railroad` | anyone | Shows your current historical rail era and how many more rails you need to place to reach the next one. |
| `/gta7jail` | op only | Sets the Custody Lock jail location to your current position. Set once by whoever runs the server. |
| `/gta7hospital` | op only | Sets CRAZY_KRANKENVAGEN's hospital delivery point to your current position. Set once by whoever runs the server. |

Everything else in the game is interaction-driven (right-click, sneak + right-click, mount) —
see below for what each one does.

## How to play

GTA7 turns the world into a living city sandbox. Everything below is a real, working system —
not a preview of planned features.

### Field Offices — claim territory, earn Flow

Find (or place) a **Beacon**. Right-click it:

- **Unclaimed** → you claim it. It starts generating **Flow** (+5 every minute) for you.
- **Held by someone else** → opens a **Contest Window** (60 seconds, longer if it's defended by
  K9 units — see below). Stand within 15 blocks of the Beacon when the timer runs out and it
  flips to you. Leave, and the current owner keeps it.
- **Held by you** → shows your current Flow balance for that office.

Claiming and fighting over Field Offices raises the area's **Watcher alertness**. Get loud enough
(claims, contests, PvP nearby) and **Enforcement** — a squad of named hostile mobs — spawns and
comes after whoever holds the office. Go quiet and alertness fades back down on its own.

Died to an Enforcement squad and respawned bare-fisted before? Every player now always respawns
with at least one wooden sword, so you can fight back immediately instead of running unarmed.

### K9 units — defend your Field Office

Tame a Wolf (vanilla Minecraft taming — bones, patience). Stand near a Field Office **you hold**,
**sneak**, and right-click your wolf: it becomes a **K9 Unit**.

Each K9 unit at a Field Office makes Contest Windows against you last longer and sets your dogs
on the challenger for the duration. Stacking more dogs still helps, just less each time
(diminishing returns) — a wall of ten wolves isn't ten times as strong as one.

### Party Stores — real trading, real hours

Find any un-designated Villager. **Sneak** + right-click it to make it a **Party Store**.

- Trade with it normally (vanilla trading, unmodified) — it remembers you and builds goodwill.
- It closes at night, same as the real Minecraft day/night cycle.
- Sustained PvP within 15 blocks force-closes it early for 5 minutes, even during the day.
- Sneak + right-click a Party Store to check its status and your goodwill with it, without
  opening a trade.

### Factions — pick a side

`/faction join <FREQUENCY|BLOC|PROCUREMENT>` — joins one of the three player-alignable factions:

| Faction | Vibe |
|---|---|
| **The Frequency** | creative power, knowledge, inner-city influence |
| **The Bloc** | working-class street stability |
| **Procurement Houses** | shadow-operator, black-market access |

Your faction colors your nametag. Claiming and flipping Field Offices earns your faction
reputation. `/faction` (no arguments) shows your current faction and reputation.

### Broadcast TVs — watch the city

`/gta7tv` places a real, floating screen at your location showing a live feed of recent city
activity — claims, flips, Contest Windows, Enforcement callouts, Party Store closures, and
CRAZY_KRANKENVAGEN runs. No resource pack or client mod needed; it's a real server-placed entity
anyone can see.

### Rogue Swarms — when a Field Office gets too loud

Push a Field Office's Watcher alertness high enough (sustained claims, contests, PvP) and instead
of just Enforcement, a **Rogue Swarm** breaks out — 3 real containment points around the office,
each with hostile mobs. Anyone can help, regardless of faction — fighting counts. Clear all 3
within 3 minutes and everyone who helped gets faction reputation. Miss the window and the Field
Office is **scarred**: it reverts to unclaimed, and Flow generation there is halved permanently
once someone reclaims it.

### Custody Lock — real consequences for losing to Enforcement

Get killed by an Enforcement squad and you're taken into custody — teleported to the server's real
jail, restricted (can't move, interact, or be hit) for 2 minutes, then released automatically.
Log out mid-sentence and you're still serving it when you log back in. (Self-KO via `/sudoku`
never triggers this — that's the escape hatch for terrain problems, not for evading a real
Enforcement kill.)

### CRAZY_KRANKENVAGEN — drive an ambulance, or catch a ride on one

Two related rescue mechanics, both real vanilla-entity systems with no custom vehicle plugin:

**The driven mission.** A wounded villager (a real `WanderingTrader`, custom-named and standing
still — "the blue guy") shows up near a Field Office. Get in a **Boat**, right-click the wounded
villager while they're within reach, and they board — your boat's real max speed is boosted for
the run (a genuine "modify the physics" ambulance, not just a normal boat). Get them to the
hospital point in time and you're rewarded with faction reputation, scaling up the longer your
delivery streak runs. A bad crash, running out of time (90 seconds), or abandoning the boat
mid-run ends the streak.

**The self-service ride.** Sometimes the wounded villager shows up with 3 llamas standing near
it. Jump on one of those llamas and it becomes a Krankenvagen on the spot — real AI kicks in, its
speed cranks up, and it heals you for the length of a short, fast ride around the terrain before
ejecting you. No boat, no hospital delivery needed — this one's automatic. It also dispatches
itself: if your health drops critically low anywhere in the world, a Krankenvagen group spawns
near you (once per minute per player) so you have a real shot at pulling through.

### Historical railroad tech tree — build your way through five real rail eras

Real vanilla rails and minecarts are the actual gameplay: place rails to progress. Five eras,
each unlocking real rail/minecart types the ones before it don't have:

| Era | Rails placed | Unlocks |
|---|---|---|
| Wooden Tramway | 0 | Plain rail |
| Iron Rail | 50 | Powered rail |
| Signal Era | 150 | Detector rail |
| Industrial Rail | 400 | Activator rail, chest/furnace/hopper minecarts |
| High-Speed Rail | 1000 | TNT minecart — the network is complete |

Placing a rail or minecart type your own era hasn't reached yet is blocked, with a message
telling you which era you need. `/railroad` shows your current era and how many more rails until
the next one. Progress is per-player, tracked by real rails placed — no separate currency, no
shop, just build.

## For developers

See `CLAUDE.md` for the stack, build/deploy workflow, and status of each milestone.
