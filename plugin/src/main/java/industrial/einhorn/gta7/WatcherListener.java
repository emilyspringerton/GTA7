package industrial.einhorn.gta7;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;

// Player-vs-player violence near a Field Office raises its alertness, same
// as claim/contest activity. "Near" = within the same radius Contest
// Windows use to resolve a flip -- one consistent notion of "at this FO".
final class WatcherListener implements Listener {

    private static final double PROXIMITY = 15.0;
    private static final int PVP_BUMP = 10;

    private final FieldOfficeManager offices;
    private final WatcherManager watchers;

    WatcherListener(FieldOfficeManager offices, WatcherManager watchers) {
        this.offices = offices;
        this.watchers = watchers;
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        for (Map.Entry<String, FieldOfficeManager.FieldOffice> entry : offices.all().entrySet()) {
            Location fo = FieldOfficeManager.locationFromKey(entry.getKey());
            if (fo == null || !fo.getWorld().equals(victim.getWorld())) continue;
            if (fo.distance(victim.getLocation()) <= PROXIMITY || fo.distance(attacker.getLocation()) <= PROXIMITY) {
                watchers.bump(fo, PVP_BUMP);
            }
        }
    }
}
