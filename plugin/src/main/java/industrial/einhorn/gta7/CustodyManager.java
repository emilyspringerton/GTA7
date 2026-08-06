package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// VS4: Custody Lock. A single holding tier for now -- TRAPX's own doc
// leaves the 10-man/Annex/1-man housing-tier split explicitly undecided
// ("Open, not decided"), so this doesn't invent an answer to a question
// the source doctrine hasn't settled either. One real jail location, one
// real timer, GameMode.SPECTATOR for the duration (can't move/interact/be
// hit -- a real restriction, not a leaky boundary check).
final class CustodyManager {

    private static final long SENTENCE_MILLIS = 2L * 60 * 1000; // 2 minutes

    record Sentence(long releaseAtMillis, GameMode originalGameMode) {}

    private final JavaPlugin plugin;
    private final File file;
    private Location jail;
    private final Map<UUID, Sentence> sentences = new HashMap<>();

    CustodyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "custody.yml");
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.contains("jail")) {
            World world = Bukkit.getWorld(yaml.getString("jail.world"));
            if (world != null) {
                jail = new Location(world, yaml.getDouble("jail.x"), yaml.getDouble("jail.y"), yaml.getDouble("jail.z"));
            }
        }
        if (yaml.contains("sentences")) {
            for (String key : yaml.getConfigurationSection("sentences").getKeys(false)) {
                long releaseAt = yaml.getLong("sentences." + key + ".releaseAt");
                GameMode original = GameMode.valueOf(yaml.getString("sentences." + key + ".original"));
                sentences.put(UUID.fromString(key), new Sentence(releaseAt, original));
            }
        }
        plugin.getLogger().info("Custody: jail " + (jail != null ? "set" : "not set") + ", "
                + sentences.size() + " active sentence(s) loaded.");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        if (jail != null) {
            yaml.set("jail.world", jail.getWorld().getName());
            yaml.set("jail.x", jail.getX());
            yaml.set("jail.y", jail.getY());
            yaml.set("jail.z", jail.getZ());
        }
        for (Map.Entry<UUID, Sentence> e : sentences.entrySet()) {
            yaml.set("sentences." + e.getKey() + ".releaseAt", e.getValue().releaseAtMillis());
            yaml.set("sentences." + e.getKey() + ".original", e.getValue().originalGameMode().name());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save custody.yml: " + ex.getMessage());
        }
    }

    void setJail(Location loc) {
        jail = loc.clone();
        save();
    }

    boolean hasJail() {
        return jail != null;
    }

    boolean isInCustody(UUID playerId) {
        return sentences.containsKey(playerId);
    }

    void sentence(Player player) {
        if (jail == null) return;
        sentences.put(player.getUniqueId(), new Sentence(System.currentTimeMillis() + SENTENCE_MILLIS, player.getGameMode()));
        player.teleport(jail);
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§4[GTA7] §fYou've been taken into custody. Release in "
                + (SENTENCE_MILLIS / 1000) + " seconds.");
        save();
    }

    // Re-applies an in-progress sentence on rejoin -- a player who logs out
    // mid-sentence and back in should still be serving it, not skip the
    // rest by disconnecting.
    void reapplyOnJoin(Player player) {
        Sentence s = sentences.get(player.getUniqueId());
        if (s == null) return;
        if (System.currentTimeMillis() >= s.releaseAtMillis()) {
            release(player.getUniqueId());
            return;
        }
        if (jail != null) player.teleport(jail);
        player.setGameMode(GameMode.SPECTATOR);
    }

    void tick() {
        long now = System.currentTimeMillis();
        for (UUID playerId : Map.copyOf(sentences).keySet()) {
            Sentence s = sentences.get(playerId);
            if (now >= s.releaseAtMillis()) {
                release(playerId);
            }
        }
    }

    private void release(UUID playerId) {
        Sentence s = sentences.remove(playerId);
        if (s == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.setGameMode(s.originalGameMode());
            player.sendMessage("§a[GTA7] §fReleased from custody.");
        }
        save();
    }
}
