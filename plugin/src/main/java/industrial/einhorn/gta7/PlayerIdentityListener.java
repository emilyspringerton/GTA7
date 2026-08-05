package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

// WOTAN integration: every connecting player gets registered into IDUNA's
// real, generic player registry (provider=minecraft, provider_sub=Bukkit
// UUID) -- the same identity system REDGARDEN-BOTS' bots already use. A
// GTA7 player and a WOTAN/SHANKPIT player are the same IDUNA player_id if
// they're the same person, even though Flow/Field-Office numbers stay in
// GTA7's own YAML for now (see IdunaClient's own doc comment for why).
final class PlayerIdentityListener implements Listener {

    private final JavaPlugin plugin;
    private final IdunaClient iduna;

    PlayerIdentityListener(JavaPlugin plugin, IdunaClient iduna) {
        this.plugin = plugin;
        this.iduna = iduna;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!iduna.isConfigured()) return;
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String playerId = iduna.registerPlayer(uuid, name);
            if (playerId != null) {
                plugin.getLogger().info(name + " linked to IDUNA player_id " + playerId);
            }
        });
    }
}
