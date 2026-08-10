# GTA7 — Product Northstar

*Codename: GTA7 ("the GTA-of-Minecraft")*
*Created: 2026-08-05*
*Built on: EINHORN_SURVIVAL (real, live Paper server, `mc.okemily.com`)*

---

## What This Document Is

Founder, real-time: "OK I WANT TO PLAY GTA7 OF MINECRAFT — use the trapx docs and the plugin
language" → `GTA7` upstream repo created for this. This is the scoping pass before any
system-level code, same convention this monorepo already uses for large asks (GOLDENBAND
integration, Bedrock Racers, Dungeon, Jungle Camps): real doc first, phased build plan, grounded
against what actually exists, not invented.

**GTA7 is not a rebuild of TRAPX.** `SHANKPIT/docs2/TRAPX_NORTHSTAR.md` describes TRAPX as a
from-scratch 3D voxel urban sandbox built on the GoblinFoxDragon engine — its own city sim,
its own RPG engine, its own client. Almost none of that exists yet (checked: no `server/watcher`,
`server/fieldoffice`, `server/k9`, etc. packages in GoblinFoxDragon as of this writing — TRAPX
is a northstar, not a built system).

GTA7 is the opposite starting point: **a real, live, already-working Minecraft server**
(EINHORN_SURVIVAL — Paper 26.2, real DNS, real Java+Bedrock crossplay, real plugin-dev pipeline
already proven with `plugins/example-plugin/`). GTA7 takes TRAPX's *doctrine* — Field Offices,
Flow, Watchers, Enforcement, K9, Media, party stores, factions, receipts — and expresses it as
real Paper plugin systems on top of real Minecraft, not custom voxel/RPG engines that don't exist
yet. Same vocabulary, same design intent, a completely different (and much shorter) path to
something playable.

---

## The Three-Sentence Version

GTA7 turns EINHORN_SURVIVAL into a living city sandbox: players claim Field Offices (real builds
in the real world) that generate Flow and draw Enforcement, real Wolves serve as K9 units, real
Villagers run Party Stores on the real day/night cycle, and a persistent, plugin-driven simulation
(Watcher alertness, Enforcement escalation, Media broadcast) reacts to what players actually do to
the server's real, player-buildable city. Everything reuses vanilla Minecraft primitives wherever
one already fits the TRAPX vocabulary (Wolves are already dogs; Villagers already trade; day/night
is already a clock) and only adds new plugin code where nothing vanilla covers the concept
(Flow currency, Field Office claim/contest, Watcher/Enforcement state machine, receipts/ledger).
The RPG layer (TRAPX's 22 GFD-derived jobs) is explicitly the furthest-out milestone — it's the
single biggest lift and the one place nothing in Minecraft or this monorepo's plugin experience
maps cleanly onto it yet.

---

## Why This Is Buildable Now (and TRAPX-on-GFD Isn't Yet)

| TRAPX system | GFD-engine status | Minecraft/Paper equivalent | Buildable now? |
|---|---|---|---|
| Voxel city world | Needs custom `ProceduralWorldStore` urban terrain, doesn't exist | Real Minecraft world — players/builders make it, or import a schematic city | Yes — zero new engine |
| Field Offices (claim/Flow/contest) | `server/fieldoffice/`, doesn't exist | New plugin system: claim a region by right-clicking a marker block, tick Flow via `BukkitScheduler`, Contest Window = timed PvP flag on the claim | Yes — straightforward Paper plugin |
| Watchers (alertness/bias/trust) | `server/watcher/`, doesn't exist | New plugin system: per-district int state, event listeners increment on `BlockBreakEvent`/`EntityDamageEvent`/claim events, decay task | Yes |
| Enforcement (cops) | `server/enforcement/`, doesn't exist | Custom-named hostile mobs (Paper's `Mob#addGoal` custom AI, real API) spawned/despawned by Watcher threshold | Yes |
| Media (broadcast) | `server/media/`, doesn't exist | Paper's `TextDisplay` entity (real, 1.19.4+) as in-world "TVs"; `BossBar` as an on-screen ticker | Yes |
| K9 Doctrine | `server/k9/`, doesn't exist | Real Minecraft Wolves — tame, name, custom NBT-tagged "K9 unit" role, `Mob#addGoal` for escort/attack behavior | Yes — closest 1:1 mapping in the whole doc |
| Rogue Swarm | `server/integrity/`, doesn't exist | Scheduled mob-horde event (reuse Paper's `Raid`-adjacent spawn APIs or a custom wave spawner) | Yes, mid-milestone |
| Party Stores | Design-only (`PARTY_STORES.md`, explicitly VS2/not-yet-built) | Real Villager trading + day/night hours (already a Minecraft primitive) + plugin-tracked stress-based closure | Yes |
| Custody Lock (jail) | Concept-capture only (TRAPX doc §"Custody Lock") | A physical structure + plugin-managed restricted-region/timer on capture | Yes, mid-milestone |
| Factions | `server/fame` (GFD, exists but for a different game) | Bukkit's native `Team`/scoreboard API + plugin-tracked reputation | Yes |
| Receipts / Ledger | `server/ledger`, doesn't exist | IDUNA — already the audit-trail authority for this entire monorepo; post GTA7 events as IDUNA Apples the same way every other repo does | Yes — no new infra |
| 22-job RPG (GFD engine) | Fully built in GFD (`server/job`, `server/combat`, `server/skillchain`, etc.) — real, tested, just not urban-flavored yet | Nothing in Minecraft/Paper maps to this — would mean hand-rolling a full class/skill/TP/skillchain system as a Paper plugin from scratch | **No — furthest-out milestone, biggest open question** |

The RPG engine is the one system GFD already has *built* that GTA7 does not. Whether GTA7 ever
gets there by hand-rolling a Paper-native version, or a future integration finds a way to bridge
IDUNA-stored GFD character data into a Minecraft session, is explicitly unresolved (see Open
Questions).

---

## Canonical Vocabulary (inherited from TRAPX, unchanged)

Field Office, Flow, Pressure, Claim, Contest Window, Custody Lock, Attention, Receipt. Never
"trap house" — same rule TRAPX's own doc states, carried over verbatim since this is the same
doctrine expressed on a different substrate.

---

## Milestones

### VS0 — Vertical Slice 0 (buildable immediately, zero new assets needed)
- One new Paper plugin (`plugin/`, this repo) added to EINHORN_SURVIVAL's `server/plugins/`.
- **Field Office claim loop**: place a marker block (e.g. a Beacon or a custom-named Shulker Box)
  → right-click to claim → claim ticks Flow (a plugin-tracked per-player/per-crew number, no new
  currency item, just a stored balance) every N seconds while held.
- **Contest Window**: any other player can right-click a held FO to open a timed PvP flag; whoever
  is standing in the claim radius when the timer expires takes ownership.
- **Receipts**: every claim/flip posts an `emily apples post`-style event to IDUNA (reuse the
  existing Apple pipeline — GTA7 becomes `source_repo=GTA7` in the same ledger every other repo
  already writes to).
- *Acceptance*: two players can physically fight over one Field Office in the live world and see
  Flow accrue/reset in real time; the flip is visible in IDUNA's Apple log.

### VS1 — Watchers + Enforcement (the city starts reacting)
- Per-district (defined as a bounding region, hand-set for now) Watcher alertness state, rising on
  claim activity/PvP/block breaking, decaying when quiet.
- At an alertness threshold, custom-named hostile mobs ("Enforcement") spawn near the district and
  path toward players holding a Field Office (Paper custom `Goal` API).
- *Acceptance*: leaving a Field Office contested and loud for 10 minutes visibly summons
  Enforcement; going quiet lets it decay back to zero.

### VS2 — K9 Doctrine + Party Stores
- Tamed Wolves can be assigned "K9 unit" status (custom NBT tag) at a Field Office — escort/guard
  behavior via `Mob#addGoal`, diminishing-returns math ported directly from TRAPX's `0.85^n`
  spec once a first K9 unit is live to test against.
- Villager-run Party Stores: real trading, real day/night hours (closed after dark, or on a
  stress-linked schedule per `PARTY_STORES.md`'s design), a plugin-tracked "merchant memory" of
  which players keep them solvent vs. bring trouble.
- *Acceptance*: a K9 unit meaningfully slows a Contest Window flip attempt; a Party Store closes
  early after sustained nearby PvP and reopens once things cool down.

### VS3 — Media + Factions
- `TextDisplay`-based in-world broadcast screens ("TVs") showing recent receipts/claims —
  the CRT-broadcast framing from TRAPX's meta-frame, minus any client-side shader work (none
  needed — `TextDisplay` is a real server-placed entity, no resource pack required).
- Faction membership via Bukkit `Team`, reputation tracked per-faction the same way Watcher state
  is tracked (plugin-side, IDUNA-backed for persistence).
- *Acceptance*: a claim/flip event appears on an in-world TV within seconds; players can see their
  faction's standing.

### VS4 — Rogue Swarms + Custody Lock
- A scheduled or Watcher-triggered mob-horde event with 3 containment objectives, matching TRAPX's
  Rogue Swarm shape (forced cross-faction cooperation, district scar on failure).
- A physical jail structure; losing a Contest Window (or a set Enforcement threshold) sends a
  player to Custody Lock — a restricted region + timer, seeded from TRAPX's 10-man/Annex/1-man
  housing-tier concept (still open there too — see TRAPX doc's own "Open, not decided" list).
- *Acceptance*: a Rogue Swarm event forces two rival factions into the same fight; a captured
  player serves real time in a real structure other players can see them in.

### VS5 — CRAZY_KRANKENVAGEN (2026-08-10)
- Real vanilla Boat entities repurposed as ambulances (Open Question 5, above, resolved in favor
  of narrative repurposing over a custom vehicle plugin) — `Boat#setMaxSpeed` (confirmed present
  in this repo's own pinned paper-api) is the real "modify the physics" lever, boosted while a
  run is active.
- A wounded-NPC spawn (an invulnerable, stationary Villager) piggybacks on `RogueSwarmManager`'s
  own "spawn something near a Field Office" pattern, exactly as this doc's earlier open-question
  pitch described. Right-click while riding a boat to pick the patient up.
- Race to a settable hospital point (`/gta7hospital`, same admin-location shape as `/gta7jail`)
  within 90s. A real crash (`VehicleBlockCollisionEvent`/`VehicleEntityCollisionEvent`) or
  abandoning the ambulance mid-run ends the run early.
- Chain runs back-to-back for an escalating reward (faction reputation, scales with current
  streak) — a bad crash or timeout resets the streak to 0. A completed/failed run is a
  `MediaManager.broadcast()` moment, same "recent city activity" framing every other GTA7 system
  already uses.
- *Acceptance*: a player can flag down (spawn), pick up, race, and deliver a patient using a real
  boat with real boosted physics, with visible chain-streak stakes.
- Implementation: `KrankenvagenManager`/`KrankenvagenListener`/`HospitalCommand`. Verified: clean
  `mvn package` build (Maven itself was missing from this box post-reboot — see this repo's own
  `CLAUDE.md` for the workaround and `sudo-queue/15-install-maven.sh` for the permanent fix), a
  clean plugin enable line in `server.log` after deploy. **Actual gameplay** (does pickup/
  delivery/crash-detection really work end to end) hasn't been exercised by a real connected
  client, same honesty convention every other GTA7 milestone's own status note already uses —
  founder should playtest and report back.

### VS6+ — RPG layer (open, not scoped yet)
Explicitly not planned in detail here — see Open Questions. Whatever shape this takes, it's the
last milestone, not an early one; VS0–VS5 all stand on their own without it.

---

## Open Questions (hold for input)

1. **RPG layer approach** — hand-roll a Paper-native class/skill system from scratch, or attempt
   to bridge GFD's already-built `server/job`/`server/combat` state (IDUNA-stored) into a
   Minecraft session somehow? No precedent in this monorepo for that kind of cross-engine bridge.
2. **District boundaries** — TRAPX's 8 scenes are pre-authored GFD voxel districts. EINHORN_SURVIVAL's
   world is real, player-shaped Minecraft terrain with no districts yet. Hand-place city-style
   builds (schematics), let the community build organically and retrofit district regions after
   the fact, or something in between?
3. **Crew/party granularity** — is Flow/Field-Office ownership per-player or per-crew (a Bukkit
   Team)? TRAPX's own doc leans crew-based but doesn't fully resolve it either.
4. **Custody Lock housing tiers** — same open question as TRAPX's own doc: what determines
   10-man/Annex/1-man assignment, how long sentences last, whether there's a sub-loop (contraband,
   escape) or it's pure social/chat space.
5. **Vehicles** — TRAPX's own Open Questions list this as unresolved too (GTA-style drivable city
   vs. foot/transit only). Minecraft has no native vehicles beyond boats/minecarts/horses.
   **Resolved 2026-08-10** for the first real vehicle mode, founder real-time: "maybe make the
   boats ambulances and modify the physics?" — repurposing real vanilla Boat entities rather than
   building a custom entity-based vehicle plugin, resolving this open question's own original
   framing ("would need either those repurposed narratively, or a custom entity-based vehicle
   plugin") in favor of the narrative-repurposing option. See VS5 below for the shipped feature
   this unblocked: **CRAZY_KRANKENVAGEN**.
6. **Relationship to TRAPX-on-GFD** — does GTA7 stay a parallel, Minecraft-native expression of
   the same doctrine indefinitely, or does it eventually get superseded once TRAPX's own voxel
   engine catches up? Not answered here — flagged so a future session doesn't assume either way.

---

*Full TRAPX doctrine reference: `SHANKPIT/docs2/TRAPX_NORTHSTAR.md`,
`SHANKPIT/docs2/PARTY_STORES.md`, `SHANKPIT/docs2/TRAPX_NEIGHBORHOOD_PERSONALITIES.md`.*
