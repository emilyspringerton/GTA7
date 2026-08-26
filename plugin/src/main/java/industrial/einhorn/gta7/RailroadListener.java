package industrial.einhorn.gta7;

import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

// VS6: historical railroad tech tree, gameplay half. See RailroadManager's
// own doc comment for the full founder-quote/design reasoning. Real
// vanilla rail/minecart blocks are the actual gameplay substrate ("powered
// by real minecraft rails") -- this listener is what actually enforces the
// tech-tree gate, not a decorative counter: placing a rail/minecart type
// your own era hasn't unlocked yet is CANCELLED, not just logged.
//
// Plain RAIL is never gated (Wooden Tramway, era 0, already allows it) --
// every placement still counts toward the next era via recordRailPlaced,
// regardless of current era.
final class RailroadListener implements Listener {

    private final RailroadManager railroad;

    RailroadListener(RailroadManager railroad) {
        this.railroad = railroad;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material type = event.getBlockPlaced().getType();
        Player player = event.getPlayer();

        if (type == Material.RAIL) {
            railroad.recordRailPlaced(player.getUniqueId());
            return;
        }
        if (type == Material.POWERED_RAIL && !railroad.hasUnlockedPoweredRail(player.getUniqueId())) {
            deny(event, player, RailroadManager.Era.IRON_RAIL);
            return;
        }
        if (type == Material.DETECTOR_RAIL && !railroad.hasUnlockedDetectorRail(player.getUniqueId())) {
            deny(event, player, RailroadManager.Era.SIGNAL_ERA);
            return;
        }
        if (type == Material.ACTIVATOR_RAIL && !railroad.hasUnlockedActivatorRailAndUtilityCarts(player.getUniqueId())) {
            deny(event, player, RailroadManager.Era.INDUSTRIAL_RAIL);
        }
    }

    // VehicleCreateEvent fires for any minecart entering the world (placed
    // by a player, spawned by a dispenser, etc.) -- Bukkit/Paper's own
    // real event for this, not a PlayerInteractEvent guess at the moment
    // of placement. It doesn't carry a placer, so a player-hostile deny
    // (removing the entity + messaging whoever's nearest) only applies
    // when a real player is close enough to plausibly be the one who just
    // placed it -- a dispenser-launched cart with nobody around is left
    // alone rather than guessed at.
    @EventHandler
    public void onVehicleCreate(VehicleCreateEvent event) {
        if (!(event.getVehicle() instanceof Minecart cart)) return;
        Material required = null;
        RailroadManager.Era requiredEra = null;
        if (cart.getType() == org.bukkit.entity.EntityType.CHEST_MINECART
                || cart.getType() == org.bukkit.entity.EntityType.FURNACE_MINECART
                || cart.getType() == org.bukkit.entity.EntityType.HOPPER_MINECART) {
            requiredEra = RailroadManager.Era.INDUSTRIAL_RAIL;
        } else if (cart.getType() == org.bukkit.entity.EntityType.TNT_MINECART) {
            requiredEra = RailroadManager.Era.HIGH_SPEED_RAIL;
        }
        if (requiredEra == null) return;

        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : cart.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(cart.getLocation());
            if (d < nearestDist && d <= 25.0) { // within 5 blocks -- plausibly the one who just placed it
                nearest = p;
                nearestDist = d;
            }
        }
        if (nearest == null) return; // no player close enough to plausibly be the placer -- leave it (e.g. dispenser)
        if (requiredEra == RailroadManager.Era.INDUSTRIAL_RAIL
                && railroad.hasUnlockedActivatorRailAndUtilityCarts(nearest.getUniqueId())) return;
        if (requiredEra == RailroadManager.Era.HIGH_SPEED_RAIL
                && railroad.hasUnlockedTntCart(nearest.getUniqueId())) return;

        event.setCancelled(true);
        nearest.sendMessage("§6[Railroad] §fThat minecart needs the §e" + requiredEra.label
                + "§f era -- keep laying rail. (/railroad to check your progress)");
    }

    private void deny(BlockPlaceEvent event, Player player, RailroadManager.Era requiredEra) {
        event.setCancelled(true);
        player.sendMessage("§6[Railroad] §fThat rail type needs the §e" + requiredEra.label
                + "§f era -- keep laying rail. (/railroad to check your progress)");
    }
}
