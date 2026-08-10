package industrial.einhorn.gta7;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

// VS5 CRAZY_KRANKENVAGEN -- see KrankenvagenManager's own doc comment for the full design.
// Handles everything that has a real Bukkit event to hook: pickup (right-click a wounded
// Villager while riding a Boat), delivery (the boat reaches the hospital), crashes
// (VehicleBlockCollisionEvent/VehicleEntityCollisionEvent -- a real "bad crash", not just any
// touch, matches the founder's own "one bad crash ends the streak" framing), and abandoning the
// ambulance mid-run (VehicleExitEvent away from the hospital). Timer expiration is the one
// transition with no natural event -- that's KrankenvagenManager#tick's own job.
final class KrankenvagenListener implements Listener {

    private final KrankenvagenManager krankenvagen;

    KrankenvagenListener(KrankenvagenManager krankenvagen) {
        this.krankenvagen = krankenvagen;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        // AbstractVillager, not Villager -- the pickup NPC is a WanderingTrader (founder: "can we
        // turn the wandering trader with the llamas into a krankenvagen?"), a Villager sibling
        // under AbstractVillager, not a Villager subtype.
        if (!(event.getRightClicked() instanceof AbstractVillager wounded)) return;
        if (!krankenvagen.isWounded(wounded.getUniqueId())) return;
        Player player = event.getPlayer();
        if (krankenvagen.hasActiveRun(player.getUniqueId())) return; // already carrying someone
        if (!(player.getVehicle() instanceof Boat boat)) {
            player.sendMessage("§6[GTA7] §fGet in a boat first -- this patient needs an ambulance, not a walk.");
            return;
        }
        if (wounded.getLocation().distance(player.getLocation()) > KrankenvagenManager.PICKUP_RADIUS) return;

        event.setCancelled(true);
        krankenvagen.pickUp(player.getUniqueId(), wounded, boat);
        player.sendMessage("§a[GTA7] §fPatient aboard -- get them to the hospital fast. "
                + "Your ambulance just got faster.");
    }

    // Founder real-time, 2026-08-10: "like the blue guy shows up with the 3 llamas and then if
    // you jump on one of the llamas it turns into a krankenvagen that does the previously
    // described interaction [heals them as it rides the player around the terrain ... at
    // incredible speed]." Separate from the boat/hospital delivery mission above -- mounting any
    // llama from an active Krankenvagen group is its own self-contained rescue, no boat needed.
    @EventHandler
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getMount() instanceof Llama llama)) return;
        if (!krankenvagen.isKrankenvagenLlama(llama.getUniqueId())) return;
        krankenvagen.startLlamaRide(player, llama);
    }

    // Founder real-time, 2026-08-10: "when a player is super low health dispatch a krankenvagen."
    // MONITOR priority + computing post-damage health from getFinalDamage() -- at the point any
    // handler runs, player.getHealth() is still the PRE-damage value; the actual HP reduction
    // happens after event processing, so this predicts the resulting health instead of reading a
    // stale one. Skips a killing blow (resultingHealth <= 0) -- a dead player doesn't need a
    // rescue llama, they need a respawn (see RespawnGearListener).
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isCancelled()) return;
        double resultingHealth = player.getHealth() - event.getFinalDamage();
        if (resultingHealth > 0 && resultingHealth <= KrankenvagenManager.LOW_HEALTH_THRESHOLD) {
            krankenvagen.dispatchForLowHealth(player);
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!krankenvagen.hasHospital()) return;
        for (Player passenger : boat.getPassengers().stream()
                .filter(Player.class::isInstance).map(Player.class::cast).toList()) {
            if (!krankenvagen.hasActiveRun(passenger.getUniqueId())) continue;
            if (boat.getLocation().distance(krankenvagen.hospital()) > KrankenvagenManager.DELIVERY_RADIUS) continue;
            krankenvagen.deliver(passenger.getUniqueId(), passenger, boat);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(event.getExited() instanceof Player player)) return;
        if (!krankenvagen.hasActiveRun(player.getUniqueId())) return;
        // Exiting near the hospital is the normal end of a successful run (onVehicleMove already
        // delivers before the player would ever exit there); exiting anywhere else mid-run is
        // abandoning the ambulance.
        krankenvagen.fail(player.getUniqueId(), player, "abandoned the ambulance", boat);
    }

    @EventHandler
    public void onVehicleBlockCollision(VehicleBlockCollisionEvent event) {
        handleCrash(event.getVehicle());
    }

    @EventHandler
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        handleCrash(event.getVehicle());
    }

    private void handleCrash(org.bukkit.entity.Vehicle vehicle) {
        if (!(vehicle instanceof Boat boat)) return;
        for (Player passenger : boat.getPassengers().stream()
                .filter(Player.class::isInstance).map(Player.class::cast).toList()) {
            if (!krankenvagen.hasActiveRun(passenger.getUniqueId())) continue;
            krankenvagen.fail(passenger.getUniqueId(), passenger, "crashed", boat);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!krankenvagen.hasActiveRun(player.getUniqueId())) return;
        krankenvagen.fail(player.getUniqueId(), null, "disconnected mid-run", null);
    }
}
