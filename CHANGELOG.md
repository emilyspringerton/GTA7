# CHANGELOG

## 2026-08-05

- feat: repo created, full scoping doc (docs/NORTHSTAR.md) mapping TRAPX doctrine onto real Paper/Minecraft plugin systems, VS0-VS4 milestone plan. Loadable plugin skeleton built, deployed to EINHORN_SURVIVAL, confirmed live in server.log.
- feat: VS0 -- Field Office claim (right-click a Beacon) + 60s Contest Window (flip if the challenger is within 15 blocks on resolution) + Flow ticking (+5/min per held FO) + /flow command. Claim/flip events post as IDUNA Apples. Deployed and confirmed enabling cleanly; gameplay itself not yet live-tested by a real player.
- feat: VS1 -- per-Field-Office Watcher alertness (claims/contests/flips/nearby PvP bump it, decays over time), Enforcement mob squads spawn and target the FO's owner at alertness >=65 (real vanilla mob AI, no custom Goal). Deployed and confirmed enabling cleanly.
- feat: real IDUNA integration -- new GTA7-SERVER agent, direct HTTP auth+Apple-posting (IdunaClient), replacing VS0's CLI-shell-out. WOTAN integration -- every joining player registered into IDUNA's real player registry (provider=minecraft), same identity system REDGARDEN-BOTS uses. Verified end-to-end via curl before wiring into Java.
- feat: VS2 -- K9 units (sneak+right-click your own tamed Wolf near a held FO) extend and defend Contest Windows with diminishing returns. Party Stores (sneak+right-click a Villager) with real day/night hours + PvP-triggered early closing + trade-based goodwill memory.
- confirmed live: a real player's join triggered a real IDUNA player_id link (WOTAN integration working end-to-end with an actual connected client, not just curl tests).
