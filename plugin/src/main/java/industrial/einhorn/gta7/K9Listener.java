package industrial.einhorn.gta7;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Map;

// Sneak + right-click your own tamed Wolf near a Field Office you hold to
// assign it as a K9 unit. Deliberately requires sneaking so it doesn't
// collide with normal wolf interactions (sitting, feeding).
final class K9Listener implements Listener {

    private static final double ASSIGN_RADIUS = 15.0;

    private final FieldOfficeManager offices;
    private final K9Manager k9;

    K9Listener(FieldOfficeManager offices, K9Manager k9) {
        this.offices = offices;
        this.k9 = k9;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (!(event.getRightClicked() instanceof Wolf wolf)) return;
        if (!wolf.isTamed() || !event.getPlayer().getUniqueId().equals(wolf.getOwnerUniqueId())) return;

        Player player = event.getPlayer();
        String nearestFo = null;
        double nearestDist = ASSIGN_RADIUS;
        for (Map.Entry<String, FieldOfficeManager.FieldOffice> entry : offices.all().entrySet()) {
            if (!entry.getValue().owner().equals(player.getUniqueId())) continue;
            Location loc = FieldOfficeManager.locationFromKey(entry.getKey());
            if (loc == null || !loc.getWorld().equals(player.getWorld())) continue;
            double dist = loc.distance(player.getLocation());
            if (dist <= nearestDist) {
                nearestDist = dist;
                nearestFo = entry.getKey();
            }
        }

        if (nearestFo == null) {
            player.sendMessage("§6[GTA7] §fNo Field Office of yours nearby to assign this K9 to.");
            return;
        }

        event.setCancelled(true);
        k9.assign(nearestFo, wolf.getUniqueId());
        wolf.setCustomName("§bK9 Unit");
        wolf.setCustomNameVisible(true);
        double score = k9.defenseScore(nearestFo);
        player.sendMessage("§6[GTA7] §fK9 unit assigned. Field Office defense score: "
                + String.format("%.2f", score));
    }
}
