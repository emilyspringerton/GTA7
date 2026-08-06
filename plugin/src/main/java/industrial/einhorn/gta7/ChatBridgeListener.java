package industrial.einhorn.gta7;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

// S171-04: relays real player chat to GFD's DragonsNShit server via
// IDUNA's existing /api/v1/chat/messages endpoint (built for the
// mud<->battlegrounds bridge, extended for gfd_server/einhorn_survival --
// see GoblinFoxDragon/docs2/CHAT_BRIDGE_TO_EINHORN_SURVIVAL_SPEC.md).
// AsyncChatEvent already fires off the main thread, so the blocking HTTP
// call in IdunaClient.postChat is safe to call directly here, no extra
// scheduling needed.
final class ChatBridgeListener implements Listener {

    private final IdunaClient iduna;

    ChatBridgeListener(IdunaClient iduna) {
        this.iduna = iduna;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!iduna.isConfigured()) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message());
        iduna.postChat(event.getPlayer().getName(), plainText);
    }
}
