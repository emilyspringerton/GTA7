package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

// Claim/flip events post to IDUNA as real Apples via IdunaClient (direct
// HTTP, authenticated as the GTA7-SERVER agent) -- replaces VS0's original
// shortcut of shelling out to the emily CLI. Async only: never block the
// main server thread on a network call.
final class ReceiptPoster {

    private final JavaPlugin plugin;
    private final IdunaClient iduna;
    private final String runId;

    ReceiptPoster(JavaPlugin plugin, IdunaClient iduna) {
        this.plugin = plugin;
        this.iduna = iduna;
        this.runId = "gta7-session-" + System.currentTimeMillis();
    }

    void post(String title, String body) {
        if (!iduna.isConfigured()) {
            plugin.getLogger().warning("IDUNA not configured (no GTA7-SERVER secret found) -- skipping receipt: " + title);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                iduna.postApple("completion", title, body, runId));
    }
}
