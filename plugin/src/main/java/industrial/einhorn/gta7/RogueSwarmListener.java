package industrial.einhorn.gta7;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

// Tracks which players actually fought a Rogue Swarm, regardless of
// faction, so RogueSwarmManager can reward everyone who helped contain it
// on success -- "forced cross-faction cooperation" per NORTHSTAR.md VS4.
final class RogueSwarmListener implements Listener {

    private final RogueSwarmManager swarms;

    RogueSwarmListener(RogueSwarmManager swarms) {
        this.swarms = swarms;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!"§4Rogue Swarm".equals(target.getCustomName())) return;
        swarms.recordParticipant(target.getUniqueId(), player.getUniqueId());
    }
}
