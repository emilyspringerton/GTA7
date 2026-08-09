# GTA7

## What this is

TRAPX doctrine (Field Offices, Flow, Watchers, Enforcement, K9, Media, Party Stores, factions,
receipts — full spec: `SHANKPIT/docs2/TRAPX_NORTHSTAR.md`) expressed as real Paper plugin systems
on top of `EINHORN_SURVIVAL`, the already-live community Minecraft server — not a rebuild of
TRAPX's own from-scratch GoblinFoxDragon voxel-city engine, which doesn't exist yet. See
`docs/NORTHSTAR.md` for the full scoping pass, milestone plan, and the system-by-system mapping
of TRAPX concepts onto real Minecraft/Paper primitives.

Founder, real-time (2026-08-05): "OK I WANT TO PLAY GTA7 OF MINECRAFT — use the trapx docs and
the plugin language."

## Stack

- **Plugin**: `plugin/` — a Paper plugin (Java, Maven), built and deployed into
  `EINHORN_SURVIVAL/server/plugins/` the same way `EINHORN_SURVIVAL/plugins/example-plugin/` was
  proven out. Not a separate server — GTA7 runs inside the real, live `mc.okemily.com`.
- **Paper API version**: must match whatever EINHORN_SURVIVAL is currently running — check
  `EINHORN_SURVIVAL/server/start.sh`'s jar filename or the server's own startup log, not this
  file (it will go stale).
- **Persistence**: IDUNA — GTA7 events (Field Office claims/flips, receipts) post as Apples the
  same way every other repo in this monorepo does. No separate database.

## Build and deploy

```bash
cd plugin
export JAVA_HOME=/home/fatbaby/EINHORN_SURVIVAL/jdk25
export PATH="$JAVA_HOME/bin:$PATH"
mvn package
cp target/gta7-*.jar /home/fatbaby/EINHORN_SURVIVAL/server/plugins/
cd /home/fatbaby/EINHORN_SURVIVAL && systemctl --user restart einhorn-survival.service
```

Check `EINHORN_SURVIVAL/server/server.log` for the plugin's enable line to confirm it actually
loaded, same verification discipline as every other plugin on this server.

## Status

**VS0 + VS1 + VS2 + VS3 + VS4 + IDUNA/WOTAN integration live** as of 2026-08-06. See `README.md`
for the player-facing how-to-play guide.

- **VS0**: right-click a Beacon to claim it as a Field Office; right-click someone else's to open
  a 60s Contest Window (flips to the challenger if within 15 blocks when it resolves); `/flow`
  shows Flow balance + Field Offices held.
- **VS1**: per-FO Watcher alertness (claims +15, Contest Windows +25, flips +15, nearby PvP +10;
  decays -5/30s). At alertness >=65, a 2-mob "Enforcement" squad spawns and targets the FO's owner
  if they're online and nearby (real vanilla mob AI via `Mob#setTarget`, no custom Goal needed).
- **IDUNA integration**: a real M2M agent, `GTA7-SERVER` (IDUNA migration
  `202608050002_gta7_server_agent.sql`, `apples.write` permission), authenticates directly over
  HTTP (`IdunaClient`) — replaced VS0's original shortcut of shelling out to the `emily` CLI.
  Claim/flip events post as real Apples.
- **WOTAN integration**: every player who joins gets registered into IDUNA's real, generic player
  registry (`provider=minecraft`, `provider_sub`=Bukkit UUID) — the same identity system
  REDGARDEN-BOTS already uses. A GTA7 player and a WOTAN/SHANKPIT player are the same IDUNA
  `player_id` if they're the same person. Flow/Field-Office numbers themselves stay in GTA7's own
  YAML for now — WOTAN's kills/deaths/sessions schema is SHANKPIT-shaped and wasn't repurposed to
  mean something else; a shared stats surface is future work, not done here.

**Auth/identity plumbing verified two ways**: direct `curl` end-to-end (auth → apple post →
player register, all real, before any Java was written against it) and a clean plugin boot with
no exceptions on the live server. **Actual gameplay** (does alertness really rise/spawn/despawn
right, does a Beacon claim really work, does a joining player really get linked) still hasn't been
exercised by a real connected client — this session has no way to connect one. Founder should
playtest and report back.

- **VS2**: sneak + right-click your own tamed Wolf near a Field Office you hold to assign it as a
  K9 unit — diminishing-returns defense score extends a Contest Window's duration and sets live
  K9s to target the challenger for its length. Sneak + right-click an un-designated Villager to
  make it a Party Store (real vanilla trading builds goodwill; closes at night or force-closes for
  5 minutes after nearby PvP).

- **VS3**: `/gta7tv` places a real `TextDisplay` entity showing a rolling feed of city activity
  (claims, flips, contests, Enforcement, Party Store closures). `/faction join
  <FREQUENCY|BLOC|PROCUREMENT>` joins one of the three player-alignable factions (real Bukkit
  Team for nametag color); claims/flips earn faction reputation.

- **VS4**: Rogue Swarms trigger automatically when a Field Office's Watcher alertness hits 90 —
  3 real containment objectives, any faction can help, all participants get faction rep on
  success; failure scars the FO (unclaimed, halved Flow once reclaimed). Dying to Enforcement
  sends a player to Custody Lock (`/gta7jail` sets the real jail location, op-only) — real
  `GameMode.SPECTATOR` restriction for 2 minutes, not a boundary check.

All 5 milestones (VS0-VS4) from `docs/NORTHSTAR.md` are now live. VS5+ (the RPG layer) was
explicitly left unscoped in the northstar and remains so.

## Founder Real-Time Direction

Whenever the founder gives real-time direction — a new ask, a correction, a "can we also..." —
route it through `emily observe -s info "Founder real-time: <summary>"` first, even if it isn't
this repo's usual domain, then sprint-plan it into `EMILY/BACKLOG.md` (`emily backlog curate`,
scoped into a real SECTION/sub-item, not just a one-line log), and only then implement. See
`EMILY/docs/THE_EMILY_WAY.md` Principle 18 ("Pave the Cow Paths").

## Commit Protocol (standing instruction)

Always commit and push completed work immediately — don't wait to be asked. This is the default
for every repo in this monorepo.
