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

**VS0 live** as of 2026-08-05: Field Office claim + Contest Window + Flow ticking, deployed and
confirmed enabling cleanly in `server.log`. Right-click a Beacon in the live world to claim it as
a Field Office; right-click someone else's to open a 60s Contest Window (flips to the challenger
if they're within 15 blocks when it resolves); `/flow` shows your Flow balance and how many
Field Offices you hold. Claim/flip events post as IDUNA Apples via the `emily` CLI (async, off
the main server thread).

**Not yet verified with a real connected client** — the plugin compiles, deploys, and enables
without error, but the actual claim/Contest Window gameplay hasn't been tested by a real player
in the live world yet (this session has no way to connect a Minecraft client itself). Founder
should try it live and report back before VS0 is considered fully proven, not just "built."

VS1+ (Watchers/Enforcement, K9/Party Stores, Media/factions, Rogue Swarms/Custody Lock) not
started — see `docs/NORTHSTAR.md`.

## Commit Protocol (standing instruction)

Always commit and push completed work immediately — don't wait to be asked. This is the default
for every repo in this monorepo.
