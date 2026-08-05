package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

// Claim/flip events post to IDUNA the same way every other repo in this
// monorepo does -- shelling out to the already-authenticated `emily` CLI
// rather than re-implementing IDUNA's HTTP auth inside the plugin. Async
// only: never block the main server thread on a subprocess.
final class ReceiptPoster {

    private static final String EMILY_BIN = "/home/fatbaby/.local/bin/emily";

    private final JavaPlugin plugin;

    ReceiptPoster(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void post(String title, String body) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        EMILY_BIN, "apples", "post", "-t", "completion", "-repo", "GTA7", title, body);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.getInputStream().readAllBytes();
                int code = proc.waitFor();
                if (code != 0) {
                    plugin.getLogger().warning("Receipt post exited " + code + " for: " + title);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Receipt post failed: " + ex.getMessage());
            }
        });
    }
}
