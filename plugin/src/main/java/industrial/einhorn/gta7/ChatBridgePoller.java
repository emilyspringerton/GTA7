package industrial.einhorn.gta7;

import industrial.einhorn.gta7.generated.ChatBridgeRules;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

// The global chat bridge (founder, 2026-11-17: "WE NEED THE CHAT BRIDGE TO WORK
// GLOBALY"): polls IDUNA's cross-game chat bus and broadcasts every OTHER
// world's lines into EINHORN_SURVIVAL's chat, tagged with where they came from
// -- [GFD] Goblin, [SHANKPIT] Freya, [SP] Pip (a SHANKPIT phone), [BIGO], ...
// -- so a bridged line is never mistaken for a player on this server. (Before:
// GFD lines only, S171-04.)
//
// Which sources show is ChatBridgeRules, generated from PARENA's
// stdlib/shankpit/chat_bridge_rules.prn -- the same rulebook SHANKPIT's server
// compiles to C -- so every world decides the same way. Minecraft is a single
// world, so it never shows its own source back.
//
// Starts from "now": the bus lists oldest first, so the first ticks page
// forward without broadcasting until a page comes back short -- a restart
// never replays history into live chat.
final class ChatBridgePoller {

    private static final Map<String, Integer> SOURCES = Map.of(
            "mud", ChatBridgeRules.srcMud(),
            "battlegrounds", ChatBridgeRules.srcBattlegrounds(),
            "gfd_server", ChatBridgeRules.srcGfd(),
            "einhorn_survival", ChatBridgeRules.srcMinecraft(),
            "bigo_server", ChatBridgeRules.srcBigo(),
            "shankpit_client", ChatBridgeRules.srcShankpitPhone(),
            "shankpit_server", ChatBridgeRules.srcShankpit());

    private static final String[] TAGS = { "?", "MUD", "BG", "GFD", "MC", "BIGO", "SP", "SHANKPIT", "TWITCH", "DISCORD", "SLACK" };

    private final JavaPlugin plugin;
    private final IdunaClient iduna;
    private long lastSeenId = 0;
    private boolean primed = false;

    ChatBridgePoller(JavaPlugin plugin, IdunaClient iduna) {
        this.plugin = plugin;
        this.iduna = iduna;
    }

    private static String tag(int src) {
        int t = ChatBridgeRules.brTag(src);
        return (t > 0 && t < TAGS.length) ? TAGS[t] : TAGS[0];
    }

    // Cut to the rulebook's limit, and drop '§' so a line from another world can't carry Minecraft
    // formatting codes (a fake "server" colour, an obfuscated line).
    private static String cut(String s, int max) {
        String clean = s.replace("§", "");
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    // Runs on an async task (registered by GTA7Plugin) -- does the blocking
    // HTTP poll off the main thread, then hops back via Bukkit.getScheduler
    // .runTask(...) to actually touch chat, since Bukkit.broadcastMessage
    // isn't safe to call from an async context.
    void tick() {
        if (!iduna.isConfigured()) return;
        int limit = ChatBridgeRules.brMaxPerPoll();
        List<IdunaClient.ChatMessage> messages = iduna.pollChat(lastSeenId, limit);

        if (!primed) {
            for (IdunaClient.ChatMessage msg : messages) lastSeenId = Math.max(lastSeenId, msg.id());
            if (messages.size() < limit) primed = true;   // reached the newest line
            return;
        }

        int self = ChatBridgeRules.srcMinecraft();
        for (IdunaClient.ChatMessage msg : messages) {
            lastSeenId = Math.max(lastSeenId, msg.id());
            int src = SOURCES.getOrDefault(msg.senderSource(), 0);
            if (ChatBridgeRules.brShows(self, src, 0) != 1) continue;
            if (msg.senderName().isEmpty() || msg.body().isEmpty()) continue;
            String name = cut(msg.senderName(), ChatBridgeRules.brNameMax());
            String body = cut(msg.body(), ChatBridgeRules.brBodyMax());
            String line = "§7[" + tag(src) + "] §f" + name + "§7: §f" + body;
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(line));
        }
    }
}
