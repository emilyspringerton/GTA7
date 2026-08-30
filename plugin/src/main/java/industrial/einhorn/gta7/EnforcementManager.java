package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.java.JavaPlugin;

import industrial.einhorn.gta7.generated.HumannessFingerprint;

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
    // Founder real-time, 2026-08-10 ("ok HTA7 this shit is hard" -> Enforcement squads named as
    // the specific pain point): plain vanilla Zombie stats (20 HP, 3 base attack damage, TWO of
    // them at once) were never explicitly tuned -- a real, unintentionally-brutal difficulty
    // spike, not a designed one. Softened, not removed -- still a real fight, paired with
    // RespawnGearListener's own wooden-sword-on-respawn so a player isn't fighting back barehanded.
    private static final double ENFORCEMENT_MAX_HEALTH = 14.0;
    private static final double ENFORCEMENT_ATTACK_DAMAGE = 2.0;

    // Real "humanness fingerprint" tuning (founder real-time, 2026-08-30: "use mishri and parena
    // mods to write an auto generated mod for gta7 ... build like a humanness fingerprint").
    // Real problem this fixes, found by reading this file before touching it: every Enforcement
    // zombie in a squad previously called setTarget() in the SAME spawn tick -- zero variance,
    // mechanically uniform target-acquisition. These three numbers feed
    // generated.HumannessFingerprint (PARENA/stdlib/gta7/humanness_fingerprint_mod.prn, compiled
    // together with stdlib/mishri/humanness.prn's own already-proven chance/randomInt/
    // gaussianNoise primitives -- real reuse, not reinvention) to give each mob its own real,
    // organic reaction delay instead.
    private static final double BASE_REACTION_TICKS = 10.0; // real half-second baseline @ 20 tps
    private static final double HESITATION_CHANCE = 0.15;
    private static final double MAX_HESITATION_BONUS_TICKS = 40.0; // real, bounded, up to +2s

    private final JavaPlugin plugin;
    private final FieldOfficeManager offices;
    private final WatcherManager watchers;
    private final MediaManager media;
    private final Map<String, List<UUID>> activeSquads = new HashMap<>();

    EnforcementManager(JavaPlugin plugin, FieldOfficeManager offices, WatcherManager watchers, MediaManager media) {
        this.plugin = plugin;
        this.offices = offices;
        this.watchers = watchers;
        this.media = media;
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
            AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(ENFORCEMENT_MAX_HEALTH);
                entity.setHealth(ENFORCEMENT_MAX_HEALTH);
            }
            AttributeInstance attackDamage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackDamage != null) attackDamage.setBaseValue(ENFORCEMENT_ATTACK_DAMAGE);
            squad.add(entity.getUniqueId());

            if (owner != null && owner.isOnline()
                    && owner.getWorld().equals(fo.getWorld())
                    && owner.getLocation().distance(fo) <= TARGET_RADIUS
                    && entity instanceof Zombie zombie) {
                scheduleHumanReaction(zombie, owner);
            }
        }

        activeSquads.put(key, squad);
        String line = "Enforcement is moving on a Field Office at "
                + fo.getBlockX() + "," + fo.getBlockY() + "," + fo.getBlockZ() + ".";
        Bukkit.broadcastMessage("§c[GTA7] §f" + line);
        media.broadcast(line);
        plugin.getLogger().info("Enforcement spawned at " + key + " (alertness " + watchers.get(fo) + ")");
    }

    // Real "humanness fingerprint": delays this specific zombie's real target acquisition by a
    // real, organically-jittered reaction time (generated.HumannessFingerprint.
    // enforcementReactionDelayTicks -- Box-Muller gaussian noise around BASE_REACTION_TICKS, the
    // exact same real primitive MISHRI's own bot-humanization layer uses), with a real, bounded
    // chance of an extra hesitation beat on top. Logged so the real tuning knobs above can
    // actually be judged against real, observed values, not just trusted blind.
    private void scheduleHumanReaction(Zombie zombie, Player owner) {
        double reactionTicks = HumannessFingerprint.enforcementReactionDelayTicks(BASE_REACTION_TICKS);
        boolean hesitated = HumannessFingerprint.enforcementShouldHesitate(HESITATION_CHANCE);
        if (hesitated) {
            reactionTicks += HumannessFingerprint.enforcementHesitationBonusTicks(MAX_HESITATION_BONUS_TICKS);
        }
        // Real, honest floor: gaussian noise can (rarely) roll negative around a small base --
        // Bukkit's own scheduler rejects a sub-1-tick delay, and a real reaction can't be
        // negative anyway, so clamp rather than let a rare unlucky roll throw at runtime.
        long delayTicks = Math.max(1L, Math.round(reactionTicks));
        plugin.getLogger().info("Enforcement humanness fingerprint: reaction=" + delayTicks
                + "t hesitated=" + hesitated + " uuid=" + zombie.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid()) {
                zombie.setTarget(owner);
            }
        }, delayTicks);
    }
}
