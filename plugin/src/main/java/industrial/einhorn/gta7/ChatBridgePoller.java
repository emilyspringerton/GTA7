package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

// S171-04: polls IDUNA for new GFD-origin chat and broadcasts it into
// EINHORN_SURVIVAL's real chat, prefixed so it's never mistaken for a
// message from a player actually on this server. Starts from "now" (highest
// existing message ID at boot), not from zero -- a restart shouldn't replay
// GFD's entire chat history into the Minecraft server.
final class ChatBridgePoller {

    private final JavaPlugin plugin;
    private final IdunaClient iduna;
    private long lastSeenId = -1; // -1 = not yet initialized

    ChatBridgePoller(JavaPlugin plugin, IdunaClient iduna) {
        this.plugin = plugin;
        this.iduna = iduna;
    }

    // Runs on an async task (registered by GTA7Plugin) -- does the blocking
    // HTTP poll off the main thread, then hops back via Bukkit.getScheduler
    // .runTask(...) to actually touch chat, since Bukkit.broadcastMessage
    // isn't safe to call from an async context.
    void tick() {
        if (!iduna.isConfigured()) return;

        if (lastSeenId < 0) {
            // First tick: find the current high-water mark without
            // broadcasting anything already-old into live chat.
            List<IdunaClient.ChatMessage> initial = iduna.pollGfdChat(0);
            lastSeenId = initial.stream().mapToLong(IdunaClient.ChatMessage::id).max().orElse(0);
            return;
        }

        List<IdunaClient.ChatMessage> messages = iduna.pollGfdChat(lastSeenId);
        if (messages.isEmpty()) return;

        for (IdunaClient.ChatMessage msg : messages) {
            lastSeenId = Math.max(lastSeenId, msg.id());
            String line = "§7[DragonsNShit] §f" + msg.senderName() + "§7: §f" + msg.body();
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(line));
        }
    }
}
