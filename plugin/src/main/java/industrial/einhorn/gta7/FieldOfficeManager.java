package industrial.einhorn.gta7;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Field Office claim/Flow state. VS0 persistence: one flat YAML file in the
// plugin's data folder (locationKey -> owner/flow). No external DB needed
// yet -- IDUNA only sees claim/flip events, not this raw state.
final class FieldOfficeManager {

    record FieldOffice(UUID owner, long flow) {}

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, FieldOffice> offices = new HashMap<>();

    FieldOfficeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "field-offices.yml");
    }

    static String keyOf(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            UUID owner = UUID.fromString(yaml.getString(key + ".owner"));
            long flow = yaml.getLong(key + ".flow");
            offices.put(key, new FieldOffice(owner, flow));
        }
        plugin.getLogger().info("Loaded " + offices.size() + " Field Office(s).");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, FieldOffice> e : offices.entrySet()) {
            yaml.set(e.getKey() + ".owner", e.getValue().owner().toString());
            yaml.set(e.getKey() + ".flow", e.getValue().flow());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save field-offices.yml: " + ex.getMessage());
        }
    }

    FieldOffice get(Location loc) {
        return offices.get(keyOf(loc));
    }

    void claim(Location loc, UUID owner) {
        offices.put(keyOf(loc), new FieldOffice(owner, 0));
        save();
    }

    void flip(Location loc, UUID newOwner) {
        offices.put(keyOf(loc), new FieldOffice(newOwner, 0));
        save();
    }

    void tickFlow(long amount) {
        boolean changed = false;
        for (Map.Entry<String, FieldOffice> e : offices.entrySet()) {
            FieldOffice fo = e.getValue();
            e.setValue(new FieldOffice(fo.owner(), fo.flow() + amount));
            changed = true;
        }
        if (changed) save();
    }

    long totalFlowFor(UUID player) {
        long total = 0;
        for (FieldOffice fo : offices.values()) {
            if (fo.owner().equals(player)) total += fo.flow();
        }
        return total;
    }

    int countOwnedBy(UUID player) {
        int count = 0;
        for (FieldOffice fo : offices.values()) {
            if (fo.owner().equals(player)) count++;
        }
        return count;
    }
}
