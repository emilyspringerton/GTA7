package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// VS1: at a Watcher alertness threshold, custom-named hostile mobs spawn
// near the Field Office and go after its owner. Vanilla mob AI (via
// Mob#setTarget) does the actual chase/attack -- no custom Goal needed,
// real Zombies already know how to hunt a target once one is set.
//
// Alertness decaying back down does NOT force-despawn an already-spawned
// squad -- they live out their vanilla lifecycle. The meter calming down
// stops new spawns, it doesn't retroactively undo the ones already out.
final class EnforcementManager {

    private static final int THRESHOLD = 65;
    private static final int SQUAD_SIZE = 2;
    private static final double TARGET_RADIUS = 40.0;

    private final JavaPlugin plugin;
    private final FieldOfficeManager offices;
    private final WatcherManager watchers;
    private final Map<String, List<UUID>> activeSquads = new HashMap<>();

    EnforcementManager(JavaPlugin plugin, FieldOfficeManager offices, WatcherManager watchers) {
        this.plugin = plugin;
        this.offices = offices;
        this.watchers = watchers;
    }

    void tick() {
        for (Map.Entry<String, FieldOfficeManager.FieldOffice> entry : offices.all().entrySet()) {
            String key = entry.getKey();
            pruneDead(key);
            if (!activeSquads.getOrDefault(key, List.of()).isEmpty()) continue;

            Location loc = FieldOfficeManager.locationFromKey(key);
            if (loc == null || !loc.isWorldLoaded()) continue;
            if (watchers.get(loc) < THRESHOLD) continue;

            spawnSquad(key, loc, entry.getValue().owner());
        }
    }

    private void pruneDead(String key) {
        List<UUID> squad = activeSquads.get(key);
        if (squad == null) return;
        List<UUID> alive = squad.stream()
                .filter(id -> Bukkit.getEntity(id) != null && Bukkit.getEntity(id).isValid())
                .collect(Collectors.toList());
        if (alive.isEmpty()) {
            activeSquads.remove(key);
        } else {
            activeSquads.put(key, alive);
        }
    }

    private void spawnSquad(String key, Location fo, UUID ownerId) {
        Player owner = Bukkit.getPlayer(ownerId);
        List<UUID> squad = new java.util.ArrayList<>();

        for (int i = 0; i < SQUAD_SIZE; i++) {
            Location spawnAt = fo.clone().add((Math.random() - 0.5) * 8, 0, (Math.random() - 0.5) * 8);
            spawnAt.setY(spawnAt.getWorld().getHighestBlockYAt(spawnAt) + 1);
            LivingEntity entity = (LivingEntity) spawnAt.getWorld().spawnEntity(spawnAt, EntityType.ZOMBIE);
            entity.setCustomName("§cEnforcement");
            entity.setCustomNameVisible(true);
            squad.add(entity.getUniqueId());

            if (owner != null && owner.isOnline()
                    && owner.getWorld().equals(fo.getWorld())
                    && owner.getLocation().distance(fo) <= TARGET_RADIUS
                    && entity instanceof Zombie zombie) {
                zombie.setTarget(owner);
            }
        }

        activeSquads.put(key, squad);
        Bukkit.broadcastMessage("§c[GTA7] §fEnforcement is moving on a Field Office at "
                + fo.getBlockX() + "," + fo.getBlockY() + "," + fo.getBlockZ() + ".");
        plugin.getLogger().info("Enforcement spawned at " + key + " (alertness " + watchers.get(fo) + ")");
    }
}
