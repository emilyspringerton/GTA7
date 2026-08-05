package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Media (VS3): real in-world broadcast screens. TextDisplay is a real,
// server-placed entity (Paper 1.19.4+) -- no resource pack, no client mod,
// the CRT-broadcast framing from NORTHSTAR.md's meta-frame section without
// any client-side shader work. A rolling feed of recent receipts, pushed to
// every registered TV whenever a new one comes in.
final class MediaManager {

    private static final int FEED_SIZE = 8;

    private final JavaPlugin plugin;
    private final Deque<String> feed = new ArrayDeque<>();
    private final List<UUID> tvs = new ArrayList<>();

    MediaManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    UUID spawnTv(Location loc) {
        TextDisplay tv = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        tv.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        tv.setLineWidth(200);
        tv.setSeeThrough(false);
        tv.setText(render());
        tvs.add(tv.getUniqueId());
        return tv.getUniqueId();
    }

    void broadcast(String line) {
        feed.addFirst(line);
        while (feed.size() > FEED_SIZE) feed.removeLast();
        refreshAll();
    }

    private void refreshAll() {
        String text = render();
        List<UUID> dead = new ArrayList<>();
        for (UUID id : tvs) {
            if (Bukkit.getEntity(id) instanceof TextDisplay tv && tv.isValid()) {
                tv.setText(text);
            } else {
                dead.add(id);
            }
        }
        tvs.removeAll(dead);
    }

    private String render() {
        return "§c§lCHANNEL 11\n§7----------------\n" +
                feed.stream().map(l -> "§f" + l).collect(Collectors.joining("\n"));
    }
}
