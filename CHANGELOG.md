# CHANGELOG

## 2026-08-26

- VS6: historical railroad tech tree shipped (S202-29). 5 real historical eras gated by rails placed, RailroadListener denies rail/minecart types below your era via BlockPlaceEvent/VehicleCreateEvent, /railroad shows progress. mvn package BUILD SUCCESS, deployed (restart queued). Apple #16062. (sess-20260825-1938-f6bd411e)


## 2026-08-25

- added auto-release CI job (PITVIPER pattern): real, non-prerelease GitHub release with the built plugin jar (sess-20260825-1938-f6bd411e)


## 2026-08-20
- docs: rewrote README.md with a full command reference table (was missing /gta7jail,
  /gta7hospital, /sudoku entirely) and a new CRAZY_KRANKENVAGEN how-to-play section (both the
  driven boat/hospital mission and the self-service llama-ride rescue) that had never made it out
  of CLAUDE.md's dev-facing Status notes. Founder: "and then please can you write the full gta7
  readme especially the commands?" (sess-20260813-2154-dda37e8b)

## 2026-08-10
- Enforcement squad 難度調整 + 重生保底木劍 + Krankenvagen（騎羊駝救援、低血量自動派遣） (sess-20260810-0505-a53abca2)

- Party Stores：TNT 保底交易 + 隨機藥水池 + 保底食物 (PartyStoreStock) (sess-20260810-0505-a53abca2)


## 2026-08-09

- feat(cmd): /sudoku self-KO command — strikeLightningEffect (cosmetic) + setHealth(0.0) for a guaranteed respawn if stuck in terrain; no killer LivingEntity involved so CustodyListener's Enforcement-kill check never fires, doesn't send the player to jail (sess-20260809-1420-e9d3d7f8)


## 2026-08-06
- Added GitHub Actions CI (build gate: JDK 21 + mvn package + jar artifact upload). No test suite exists yet; real gameplay verification needs a connected client CI can't provide.
- Recorded the founder's CRAZY_KRANKENVAGEN (ambulance/paramedic-mission) vehicle concept in docs/NORTHSTAR.md's Open Questions -- real GTA Paramedic-mission tradition, mapped onto GTA7's existing RogueSwarmManager/MediaManager patterns. Not built -- depends on the still-unresolved general vehicle question. (sess-20260723-2347-df115bd5)
- S171-04 chat bridge, EINHORN_SURVIVAL side: ChatBridgeListener posts real player chat to IDUNA, ChatBridgePoller relays GFD-origin messages into Minecraft chat prefixed [DragonsNShit]. Verified end-to-end via curl before writing Java, deployed, confirmed enabling cleanly. GFD side not yet built. (sess-20260723-2347-df115bd5)

- VS4 -- Rogue Swarms (triggers at alertness 90, 3 containment objectives, cross-faction reward, district scar on failure) + Custody Lock (Enforcement-kill sends players to a real jail, GameMode.SPECTATOR restriction, /gta7jail sets the location). All 5 NORTHSTAR.md milestones now live. (sess-20260723-2347-df115bd5)


## 2026-08-05

- feat: repo created, full scoping doc (docs/NORTHSTAR.md) mapping TRAPX doctrine onto real Paper/Minecraft plugin systems, VS0-VS4 milestone plan. Loadable plugin skeleton built, deployed to EINHORN_SURVIVAL, confirmed live in server.log.
- feat: VS0 -- Field Office claim (right-click a Beacon) + 60s Contest Window (flip if the challenger is within 15 blocks on resolution) + Flow ticking (+5/min per held FO) + /flow command. Claim/flip events post as IDUNA Apples. Deployed and confirmed enabling cleanly; gameplay itself not yet live-tested by a real player.
- feat: VS1 -- per-Field-Office Watcher alertness (claims/contests/flips/nearby PvP bump it, decays over time), Enforcement mob squads spawn and target the FO's owner at alertness >=65 (real vanilla mob AI, no custom Goal). Deployed and confirmed enabling cleanly.
- feat: real IDUNA integration -- new GTA7-SERVER agent, direct HTTP auth+Apple-posting (IdunaClient), replacing VS0's CLI-shell-out. WOTAN integration -- every joining player registered into IDUNA's real player registry (provider=minecraft), same identity system REDGARDEN-BOTS uses. Verified end-to-end via curl before wiring into Java.
- feat: VS2 -- K9 units (sneak+right-click your own tamed Wolf near a held FO) extend and defend Contest Windows with diminishing returns. Party Stores (sneak+right-click a Villager) with real day/night hours + PvP-triggered early closing + trade-based goodwill memory.
- confirmed live: a real player's join triggered a real IDUNA player_id link (WOTAN integration working end-to-end with an actual connected client, not just curl tests).
- feat: VS3 -- /gta7tv places a real TextDisplay broadcast screen showing recent city activity (claims/flips/contests/Enforcement/store closures). /faction join <FREQUENCY|BLOC|PROCUREMENT> -- real Bukkit Team nametag color + reputation from claims/flips.
- docs: added README.md with connection instructions + full how-to-play guide for every system shipped so far.
