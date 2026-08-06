package industrial.einhorn.gta7;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

// VS4: dying to Enforcement sends a player to Custody Lock -- one of the
// two triggers NORTHSTAR.md VS4 names ("losing a Contest Window (or a set
// Enforcement threshold)"). Enforcement-kill is the cleaner of the two to
// detect (a real death event with a real named killer) and doesn't require
// re-litigating Contest Window's own resolution logic.
final class CustodyListener implements Listener {

    private final CustodyManager custody;

    CustodyListener(CustodyManager custody) {
        this.custody = custody;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!custody.hasJail()) return;
        if (!(event.getEntity().getKiller() instanceof LivingEntity killer)) return;
        if (!"§cEnforcement".equals(killer.getCustomName())) return;
        custody.sentence(event.getEntity());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        custody.reapplyOnJoin(event.getPlayer());
    }
}
