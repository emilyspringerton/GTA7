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

Scoping pass only as of 2026-08-05 (`docs/NORTHSTAR.md`, VS0 milestone defined). Plugin directory
is a minimal loadable skeleton — no Field Office/Watcher/K9/Media systems implemented yet.

## Commit Protocol (standing instruction)

Always commit and push completed work immediately — don't wait to be asked. This is the default
for every repo in this monorepo.
