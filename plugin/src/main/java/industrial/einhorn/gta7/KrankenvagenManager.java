package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// VS5: CRAZY_KRANKENVAGEN. Founder, real-time, docs/NORTHSTAR.md §5 ("Open Questions", recorded
// but explicitly not committed as a build target there): "CRAZY_KRANKENVAGEN" -- Krankenwagen is
// German for ambulance, mapped onto GTA's own real Paramedic missions (III/Vice City/San
// Andreas): drive an ambulance, pick up a critically injured NPC, race them to a hospital point
// before a timer runs out, chain runs back-to-back for an escalating reward, one bad crash ends
// the streak. Committed as a real build target 2026-08-10 -- founder resolved the northstar's
// own blocking prerequisite ("this pitch assumes some kind of drivable, not-a-vanilla-boat
// vehicle already exists") real-time: "maybe make the boats ambulances and modify the physics."
// Real vanilla Boat entities, no custom vehicle plugin needed -- Boat#setMaxSpeed (confirmed
// present in this repo's own pinned paper-api 26.2.build.97-stable) is the real "modify the
// physics" lever, boosted while a run is active.
//
// Piggybacks on RogueSwarmManager's own "spawn something near a Field Office" pattern per the
// northstar's own pitch, and a completed/failed run is a real MediaManager.broadcast() moment,
// same "recent city activity" framing every other GTA7 system already uses. Reward is faction
// reputation (FactionManager#addRep), not Flow -- Flow is tied to owning a Field Office
// (FieldOfficeManager), and a paramedic run should be available to any player regardless of
// whether they hold one, same reasoning RogueSwarmManager already uses reputation for its own
// any-faction-can-help containment reward.
final class KrankenvagenManager {

    static final double PICKUP_RADIUS = 4.0;
    static final double DELIVERY_RADIUS = 5.0;
    private static final long RUN_TIMEOUT_MILLIS = 90L * 1000; // 90s -- a real race, not a stroll
    private static final double BOAT_SPEED_MULTIPLIER = 1.6; // "modify the physics" -- a real ambulance should outrun a normal boat
    private static final int REP_BASE = 15;
    private static final int REP_PER_STREAK = 5; // escalating reward, same "chain runs" framing as the founder's own pitch

    // Founder real-time, 2026-08-10: "can we turn the wandering trader with the llamas into a
    // krankenvagen?" -> "like the blue guy shows up with the 3 llamas and then if you jump on one
    // of the llamas it turns into a krankenvagen that does the previously described interaction"
    // -> "it heals them as it rides the player around the terrain on one of the llamas at
    // incredible speed" -> "when a player is super low health dispatch a krankenvagen." A second,
    // self-contained rescue mechanic alongside the original boat/hospital delivery mission above:
    // mounting a Krankenvagen llama re-enables its AI (spawned with AI off so the group stays put
    // as a visible landmark until someone interacts with it, same idiom the boat-pickup trader
    // already used), cranks its real MOVEMENT_SPEED attribute, and heals the rider on a short
    // repeating tick for a fixed ride duration before ejecting them -- no hospital delivery point
    // needed, this is an automatic/self-service rescue, not a driven mission.
    private static final int LLAMAS_PER_GROUP = 3;
    private static final long RIDE_DURATION_MILLIS = 10_000; // 10s -- long enough to feel like a real ride, short enough to stay a burst not a permanent mount
    private static final long RIDE_HEAL_PERIOD_TICKS = 20; // every 1s
    private static final double RIDE_HEAL_PER_TICK = 2.0; // 1 heart/s -- meaningful but not an instant full heal, still a real ride
    private static final double RIDE_MOVEMENT_SPEED = 0.6; // vanilla llama baseline is ~0.175 -- "incredible speed" is a real, large multiple of that, not a token bump
    static final double LOW_HEALTH_THRESHOLD = 4.0; // 2 hearts -- package-visible, KrankenvagenListener's own damage-monitor check reads this
    private static final long LOW_HEALTH_DISPATCH_COOLDOWN_MILLIS = 60_000; // per player -- a dispatch call, not a heal-on-demand button spammable every tick

    record ActiveRun(UUID woundedMobId, long startedAtMillis, double originalBoatMaxSpeed) {}

    private final JavaPlugin plugin;
    private final FieldOfficeManager offices;
    private final FactionManager factions;
    private final MediaManager media;
    private final File file;
    private final Random random = new Random();

    private Location hospital;
    private UUID activeWoundedMobId; // one at a time, real MVP scope -- multiple concurrent victims is real, later depth
    private final Map<UUID, ActiveRun> activeRuns = new HashMap<>(); // playerId -> run
    private final Map<UUID, Integer> streaks = new HashMap<>(); // playerId -> current chain length
    private final Set<UUID> krankenvagenLlamaIds = new HashSet<>(); // llamas eligible to trigger a ride on mount
    private final Map<UUID, Long> lowHealthDispatchCooldowns = new HashMap<>(); // playerId -> last dispatch millis

    KrankenvagenManager(JavaPlugin plugin, FieldOfficeManager offices, FactionManager factions, MediaManager media) {
        this.plugin = plugin;
        this.offices = offices;
        this.factions = factions;
        this.media = media;
        this.file = new File(plugin.getDataFolder(), "krankenvagen.yml");
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.contains("hospital")) {
            World world = Bukkit.getWorld(yaml.getString("hospital.world"));
            if (world != null) {
                hospital = new Location(world, yaml.getDouble("hospital.x"), yaml.getDouble("hospital.y"), yaml.getDouble("hospital.z"));
            }
        }
        if (yaml.contains("streaks")) {
            for (String key : yaml.getConfigurationSection("streaks").getKeys(false)) {
                streaks.put(UUID.fromString(key), yaml.getInt("streaks." + key));
            }
        }
        plugin.getLogger().info("Krankenvagen: hospital " + (hospital != null ? "set" : "not set")
                + ", " + streaks.size() + " player streak(s) loaded.");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        if (hospital != null) {
            yaml.set("hospital.world", hospital.getWorld().getName());
            yaml.set("hospital.x", hospital.getX());
            yaml.set("hospital.y", hospital.getY());
            yaml.set("hospital.z", hospital.getZ());
        }
        for (Map.Entry<UUID, Integer> e : streaks.entrySet()) {
            yaml.set("streaks." + e.getKey(), e.getValue());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save krankenvagen.yml: " + ex.getMessage());
        }
    }

    void setHospital(Location loc) {
        hospital = loc.clone();
        save();
    }

    boolean hasHospital() {
        return hospital != null;
    }

    Location hospital() {
        return hospital;
    }

    boolean isWounded(UUID mobId) {
        return mobId.equals(activeWoundedMobId);
    }

    boolean hasActiveRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    int streak(UUID playerId) {
        return streaks.getOrDefault(playerId, 0);
    }

    // pickUp: the wounded NPC boards the ambulance -- real MVP scope removes the villager rather
    // than making it a real boat passenger (chest-boat-style multi-passenger riding is fragile
    // across client versions and isn't the point of the mechanic), and boosts the boat's real
    // max speed for the duration, the "modify the physics" half of the founder's own pitch.
    void pickUp(UUID playerId, AbstractVillager wounded, Boat boat) {
        double original = boat.getMaxSpeed();
        boat.setMaxSpeed(original * BOAT_SPEED_MULTIPLIER);
        activeRuns.put(playerId, new ActiveRun(wounded.getUniqueId(), System.currentTimeMillis(), original));
        activeWoundedMobId = null;
        wounded.remove();
    }

    // deliver: a successful run -- reward scales with the player's own current streak (already
    // incremented before this is called), same escalating-chain framing the founder's own pitch
    // uses ("chain runs back-to-back for an escalating reward").
    void deliver(UUID playerId, org.bukkit.entity.Player player, Boat boat) {
        ActiveRun run = activeRuns.remove(playerId);
        if (run == null) return;
        boat.setMaxSpeed(run.originalBoatMaxSpeed());
        int streak = streaks.merge(playerId, 1, Integer::sum);
        int reward = REP_BASE + REP_PER_STREAK * (streak - 1);
        factions.addRep(playerId, reward);
        player.sendMessage("§a[GTA7] §fDelivered! +" + reward + " reputation. Streak: " + streak + ".");
        String line = player.getName() + " delivered a patient to the hospital -- streak " + streak + ", +" + reward + " reputation.";
        Bukkit.broadcastMessage("§a§l[GTA7] §f" + line);
        media.broadcast(line);
        save();
    }

    // fail: timeout, crash, or abandoning the ambulance mid-run -- ends the streak, same "one
    // bad crash ends the streak" framing the founder's own pitch uses.
    void fail(UUID playerId, org.bukkit.entity.Player player, String reason, Boat boat) {
        ActiveRun run = activeRuns.remove(playerId);
        if (run == null) return;
        if (boat != null && boat.isValid()) boat.setMaxSpeed(run.originalBoatMaxSpeed());
        int lostStreak = streaks.put(playerId, 0);
        if (player != null) {
            player.sendMessage("§4[GTA7] §fRun failed (" + reason + ")."
                    + (lostStreak > 0 ? " Streak of " + lostStreak + " lost." : ""));
        }
        String line = (player != null ? player.getName() : "A responder") + "'s ambulance run failed (" + reason + ").";
        media.broadcast(line);
        save();
    }

    // tick: timer expiration + spawning a fresh wounded NPC when none is active. Delivery
    // success/failure-on-exit are event-driven (KrankenvagenListener); this handles the
    // transition that has no natural Bukkit event -- "time simply passed."
    void tick() {
        if (hospital == null) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, ActiveRun> entry : Map.copyOf(activeRuns).entrySet()) {
            if (now - entry.getValue().startedAtMillis() < RUN_TIMEOUT_MILLIS) continue;
            org.bukkit.entity.Player player = Bukkit.getPlayer(entry.getKey());
            Boat boat = (player != null && player.getVehicle() instanceof Boat b) ? b : null;
            fail(entry.getKey(), player, "ran out of time", boat);
        }

        if (activeWoundedMobId != null && Bukkit.getEntity(activeWoundedMobId) != null
                && Bukkit.getEntity(activeWoundedMobId).isValid()) {
            return; // one at a time, real MVP scope -- see this class's own field doc comment
        }
        activeWoundedMobId = null;
        spawnWoundedNearAFieldOffice();
    }

    boolean isKrankenvagenLlama(UUID llamaId) {
        return krankenvagenLlamaIds.contains(llamaId);
    }

    // startLlamaRide: the mount-triggered rescue. Removes the llama from the eligible set
    // immediately (one ride per llama, not re-triggerable mid-ride), re-enables AI so vanilla
    // wander behavior actually carries the rider around ("rides the player around the terrain"),
    // cranks its real movement speed, then a repeating task heals the rider and, once
    // RIDE_DURATION_MILLIS elapses, ejects them and removes the llama (its job is done -- an
    // ambient wandering llama with a rider-facing custom name left behind would be confusing).
    void startLlamaRide(Player player, Llama llama) {
        krankenvagenLlamaIds.remove(llama.getUniqueId());
        llama.setAI(true);
        AttributeInstance speed = llama.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(RIDE_MOVEMENT_SPEED);
        player.sendMessage("§a[GTA7] §fKrankenvagen! Hang on.");

        long startedAt = System.currentTimeMillis();
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!llama.isValid() || !player.isOnline() || !player.equals(llama.getPassenger())) {
                task.cancel();
                if (llama.isValid()) llama.remove();
                return;
            }
            double newHealth = Math.min(player.getHealth() + RIDE_HEAL_PER_TICK, maxHealthOf(player));
            player.setHealth(newHealth);
            if (System.currentTimeMillis() - startedAt >= RIDE_DURATION_MILLIS) {
                task.cancel();
                llama.eject();
                llama.remove();
                player.sendMessage("§a[GTA7] §fThe Krankenvagen peels off -- you're patched up.");
            }
        }, 0L, RIDE_HEAL_PERIOD_TICKS);
    }

    private static double maxHealthOf(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth != null ? maxHealth.getValue() : 20.0;
    }

    // dispatchForLowHealth: founder, real-time -- "when a player is super low health dispatch a
    // krankenvagen." Same group spawn as the Field-Office wounded pickup, but centered on the
    // struggling player and NOT registered as the boat-delivery objective (registerAsWoundedPickup
    // = false) -- this is the automatic-rescue flavor, the player just needs to reach one of the
    // 3 llamas that show up, not drive a boat anywhere.
    void dispatchForLowHealth(Player player) {
        long now = System.currentTimeMillis();
        Long last = lowHealthDispatchCooldowns.get(player.getUniqueId());
        if (last != null && now - last < LOW_HEALTH_DISPATCH_COOLDOWN_MILLIS) return;
        lowHealthDispatchCooldowns.put(player.getUniqueId(), now);

        double angle = random.nextDouble() * 2 * Math.PI;
        Location spawnLoc = player.getLocation().clone().add(Math.cos(angle) * 6.0, 0, Math.sin(angle) * 6.0);
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
        spawnKrankenvagenGroup(spawnLoc, false);

        player.sendMessage("§c[GTA7] §fYou're critically low -- a Krankenvagen has been dispatched. Jump on a llama!");
        String line = "A Krankenvagen was dispatched for " + player.getName() + " -- critical condition.";
        media.broadcast(line);
    }

    // spawnKrankenvagenGroup: the real vanilla WanderingTrader (founder: "the blue guy") plus
    // LLAMAS_PER_GROUP real TRADER_LLAMA, spawned close together so the visual actually reads as
    // a group -- no leash linkage (Trader Llamas don't naturally look leashed in vanilla either,
    // they just travel near the trader via its own AI goal, not exposed through public API).
    // registerAsWoundedPickup controls whether the TRADER itself becomes the boat-delivery
    // objective (Field-Office spawns) or is purely decorative flavor around the llamas
    // (low-health dispatch spawns -- see dispatchForLowHealth's own doc comment).
    private void spawnKrankenvagenGroup(Location center, boolean registerAsWoundedPickup) {
        AbstractVillager trader = (AbstractVillager) center.getWorld().spawnEntity(center, EntityType.WANDERING_TRADER);
        trader.setCustomName("§c§lWOUNDED -- needs an ambulance");
        trader.setCustomNameVisible(true);
        trader.setAI(false); // stays put -- a landmark, not a fleeing NPC
        trader.setInvulnerable(true);
        if (registerAsWoundedPickup) {
            activeWoundedMobId = trader.getUniqueId();
        }

        for (int i = 0; i < LLAMAS_PER_GROUP; i++) {
            Location llamaLoc = center.clone().add((random.nextDouble() - 0.5) * 3, 0, (random.nextDouble() - 0.5) * 3);
            Llama llama = (Llama) center.getWorld().spawnEntity(llamaLoc, EntityType.TRADER_LLAMA);
            llama.setAI(false); // re-enabled on mount, see startLlamaRide
            llama.setInvulnerable(true);
            krankenvagenLlamaIds.add(llama.getUniqueId());
        }
    }

    private void spawnWoundedNearAFieldOffice() {
        var claimed = offices.all();
        if (claimed.isEmpty()) return;
        int pick = random.nextInt(claimed.size());
        String foKey = claimed.keySet().stream().skip(pick).findFirst().orElse(null);
        if (foKey == null) return;
        Location fo = FieldOfficeManager.locationFromKey(foKey);
        if (fo == null || !fo.isWorldLoaded()) return;

        double angle = random.nextDouble() * 2 * Math.PI;
        Location spawnLoc = fo.clone().add(Math.cos(angle) * 10.0, 0, Math.sin(angle) * 10.0);
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        spawnKrankenvagenGroup(spawnLoc, true);

        String line = "A critically injured patient needs an ambulance near a Field Office ("
                + fmtKey(foKey) + ").";
        media.broadcast(line);
    }

    private static String fmtKey(String foKey) {
        String[] parts = foKey.split(":");
        return parts.length == 4 ? parts[1] + "," + parts[2] + "," + parts[3] : foKey;
    }
}
