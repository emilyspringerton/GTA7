package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Factions (VS3), per NORTHSTAR.md's Faction Reputation table -- only the
// three player-alignable factions (The Frequency, The Bloc, Procurement
// Houses). Oversight Sects and Media Apparatus are NPC-only in the doc, not
// joinable, so they're not in this enum. Membership via a real Bukkit Team
// (nametag color) + a plugin-tracked reputation counter, YAML-persisted.
final class FactionManager {

    enum Faction {
        FREQUENCY("The Frequency", ChatColor.LIGHT_PURPLE),
        BLOC("The Bloc", ChatColor.YELLOW),
        PROCUREMENT("Procurement Houses", ChatColor.DARK_AQUA);

        final String displayName;
        final ChatColor color;

        Faction(String displayName, ChatColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        static Faction parse(String s) {
            for (Faction f : values()) {
                if (f.name().equalsIgnoreCase(s) || f.displayName.equalsIgnoreCase(s)) return f;
            }
            return null;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Faction> membership = new HashMap<>();
    private final Map<UUID, Integer> reputation = new HashMap<>();

    FactionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "factions.yml");
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            UUID id = UUID.fromString(key);
            Faction f = Faction.parse(yaml.getString(key + ".faction"));
            if (f != null) membership.put(id, f);
            reputation.put(id, yaml.getInt(key + ".rep"));
        }
        plugin.getLogger().info("Loaded " + membership.size() + " faction membership(s).");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (UUID id : membership.keySet()) {
            yaml.set(id + ".faction", membership.get(id).name());
            yaml.set(id + ".rep", reputation.getOrDefault(id, 0));
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save factions.yml: " + ex.getMessage());
        }
    }

    Faction get(UUID playerId) {
        return membership.get(playerId);
    }

    int reputation(UUID playerId) {
        return reputation.getOrDefault(playerId, 0);
    }

    void join(Player player, Faction faction) {
        membership.put(player.getUniqueId(), faction);
        reputation.putIfAbsent(player.getUniqueId(), 0);
        applyTeam(player, faction);
        save();
    }

    void addRep(UUID playerId, int amount) {
        if (!membership.containsKey(playerId)) return;
        reputation.merge(playerId, amount, Integer::sum);
        save();
    }

    // Re-applies the player's saved faction team on join, since Bukkit teams
    // aren't themselves persisted across a server restart the way YAML is.
    void applyOnJoin(Player player) {
        Faction f = membership.get(player.getUniqueId());
        if (f != null) applyTeam(player, f);
    }

    private void applyTeam(Player player, Faction faction) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Faction f : Faction.values()) {
            Team existing = board.getTeam(f.name());
            if (existing != null) existing.removeEntry(player.getName());
        }
        Team team = board.getTeam(faction.name());
        if (team == null) {
            team = board.registerNewTeam(faction.name());
            team.setColor(faction.color);
        }
        team.addEntry(player.getName());
    }
}
