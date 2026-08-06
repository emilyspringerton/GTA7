package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// VS4: a Watcher-triggered mob-horde event with 3 containment objectives,
// matching TRAPX's Rogue Swarm shape (forced cross-faction cooperation,
// district scar on failure). Triggers at a higher alertness bar than
// Enforcement (VS1) -- Enforcement is the city pushing back; a Rogue Swarm
// is the city actually breaking, and only fires when Enforcement alone
// hasn't cooled things down.
final class RogueSwarmManager {

    private static final int ROGUE_THRESHOLD = 90;
    private static final int OBJECTIVE_COUNT = 3;
    private static final double OBJECTIVE_RADIUS = 12.0;
    private static final int MOBS_PER_OBJECTIVE = 2;
    private static final long MAX_DURATION_MILLIS = 3L * 60 * 1000; // 3 minutes
    private static final int REP_REWARD = 20;

    record Swarm(List<List<UUID>> objectiveMobs, long startedAtMillis, Set<UUID> participants) {}

    private final JavaPlugin plugin;
    private final FieldOfficeManager offices;
    private final WatcherManager watchers;
    private final MediaManager media;
    private final FactionManager factions;
    private final Map<String, Swarm> activeSwarms = new HashMap<>();

    RogueSwarmManager(JavaPlugin plugin, FieldOfficeManager offices, WatcherManager watchers,
                       MediaManager media, FactionManager factions) {
        this.plugin = plugin;
        this.offices = offices;
        this.watchers = watchers;
        this.media = media;
        this.factions = factions;
    }

    void recordParticipant(UUID mobId, UUID playerId) {
        for (Swarm swarm : activeSwarms.values()) {
            for (List<UUID> mobs : swarm.objectiveMobs()) {
                if (mobs.contains(mobId)) {
                    swarm.participants().add(playerId);
                    return;
                }
            }
        }
    }

    void tick() {
        // Resolve active swarms first (success or timeout) before checking
        // for new ones, so a just-contained swarm's FO is free to re-trigger
        // later rather than double-counted this same tick.
        List<String> resolved = new ArrayList<>();
        for (Map.Entry<String, Swarm> entry : activeSwarms.entrySet()) {
            String foKey = entry.getKey();
            Swarm swarm = entry.getValue();
            boolean contained = swarm.objectiveMobs().stream().allMatch(this::allDead);
            boolean expired = System.currentTimeMillis() - swarm.startedAtMillis() >= MAX_DURATION_MILLIS;

            if (contained) {
                resolveSuccess(foKey, swarm);
                resolved.add(foKey);
            } else if (expired) {
                resolveFailure(foKey, swarm);
                resolved.add(foKey);
            }
        }
        resolved.forEach(activeSwarms::remove);

        for (Map.Entry<String, FieldOfficeManager.FieldOffice> entry : offices.all().entrySet()) {
            String foKey = entry.getKey();
            if (activeSwarms.containsKey(foKey)) continue;
            Location loc = FieldOfficeManager.locationFromKey(foKey);
            if (loc == null || !loc.isWorldLoaded()) continue;
            if (watchers.get(loc) < ROGUE_THRESHOLD) continue;
            trigger(foKey, loc);
        }
    }

    private boolean allDead(List<UUID> mobIds) {
        return mobIds.stream().noneMatch(id -> Bukkit.getEntity(id) != null && Bukkit.getEntity(id).isValid());
    }

    private void trigger(String foKey, Location fo) {
        List<List<UUID>> objectiveMobs = new ArrayList<>();
        for (int i = 0; i < OBJECTIVE_COUNT; i++) {
            double angle = (2 * Math.PI / OBJECTIVE_COUNT) * i;
            Location objLoc = fo.clone().add(Math.cos(angle) * OBJECTIVE_RADIUS, 0, Math.sin(angle) * OBJECTIVE_RADIUS);
            objLoc.setY(objLoc.getWorld().getHighestBlockYAt(objLoc) + 1);

            List<UUID> mobs = new ArrayList<>();
            for (int m = 0; m < MOBS_PER_OBJECTIVE; m++) {
                EntityType type = (m % 2 == 0) ? EntityType.ZOMBIE : EntityType.SKELETON;
                LivingEntity entity = (LivingEntity) objLoc.getWorld().spawnEntity(objLoc, type);
                entity.setCustomName("§4Rogue Swarm");
                entity.setCustomNameVisible(true);
                mobs.add(entity.getUniqueId());
            }
            objectiveMobs.add(mobs);
        }

        activeSwarms.put(foKey, new Swarm(objectiveMobs, System.currentTimeMillis(), new HashSet<>()));

        String line = "A Rogue Swarm has broken out at a Field Office ("
                + fmtKey(foKey) + ") -- 3 objectives, all factions, contain it before it scars the district.";
        Bukkit.broadcastMessage("§4§l[GTA7] §f" + line);
        media.broadcast(line);
        plugin.getLogger().info("Rogue Swarm triggered at " + foKey);
    }

    private void resolveSuccess(String foKey, Swarm swarm) {
        for (UUID playerId : swarm.participants()) {
            factions.addRep(playerId, REP_REWARD);
        }
        String line = "Rogue Swarm at " + fmtKey(foKey) + " contained by " + swarm.participants().size()
                + " responder(s), regardless of faction. District holds.";
        Bukkit.broadcastMessage("§a§l[GTA7] §f" + line);
        media.broadcast(line);
    }

    private void resolveFailure(String foKey, Swarm swarm) {
        offices.scar(foKey);
        for (List<UUID> mobs : swarm.objectiveMobs()) {
            for (UUID mobId : mobs) {
                if (Bukkit.getEntity(mobId) instanceof LivingEntity le && le.isValid()) {
                    le.remove();
                }
            }
        }
        String line = "Rogue Swarm at " + fmtKey(foKey) + " was not contained in time -- the Field Office is scarred and reverts to unclaimed. Flow there is halved once reclaimed.";
        Bukkit.broadcastMessage("§4§l[GTA7] §f" + line);
        media.broadcast(line);
    }

    private static String fmtKey(String foKey) {
        String[] parts = foKey.split(":");
        return parts.length == 4 ? parts[1] + "," + parts[2] + "," + parts[3] : foKey;
    }
}
