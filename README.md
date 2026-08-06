# GTA7

TRAPX doctrine, live on a real Minecraft server. GTA7 is a plugin running inside
**EINHORN_SURVIVAL** (`mc.okemily.com`) — there's no separate server or client to install.

## Connecting

**Java Edition**: `mc.okemily.com` — plain address, no port needed.

**Bedrock Edition** (phone/console/Windows): also `mc.okemily.com`, but type the port
explicitly: **`19133`**. Bedrock's "Add Server" screen has a separate port field for this.

Access is open — real account required (no cracked/offline clients), no whitelist. See
`EINHORN_SURVIVAL/README.md` for the full connection details and server status commands.

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

`/flow` — check your total Flow balance and how many Field Offices you hold across the map.

Claiming and fighting over Field Offices raises the area's **Watcher alertness**. Get loud enough
(claims, contests, PvP nearby) and **Enforcement** — a squad of named hostile mobs — spawns and
comes after whoever holds the office. Go quiet and alertness fades back down on its own.

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
activity — claims, flips, Contest Windows, Enforcement callouts, Party Store closures. No
resource pack or client mod needed; it's a real server-placed entity anyone can see.

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
Log out mid-sentence and you're still serving it when you log back in.

## For developers

See `CLAUDE.md` for the stack, build/deploy workflow, and status of each milestone.
