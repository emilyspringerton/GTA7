package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

    // scarred = true after a failed Rogue Swarm containment (VS4) -- halves
    // future Flow generation as a lasting consequence, not just cosmetic.
    record FieldOffice(UUID owner, long flow, boolean scarred) {}

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

    // Inverse of keyOf -- lets EnforcementManager go from a tracked key back
    // to a real Location to spawn mobs at. Returns null if the world isn't
    // loaded (shouldn't happen for this server's single world, but cheap to
    // guard against rather than assume).
    static Location locationFromKey(String key) {
        String[] parts = key.split(":");
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    // Real Location + owner pairs for every known Field Office, for systems
    // (EnforcementManager) that need to act on the world, not just the map.
    Map<String, FieldOffice> all() {
        return Map.copyOf(offices);
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            UUID owner = UUID.fromString(yaml.getString(key + ".owner"));
            long flow = yaml.getLong(key + ".flow");
            boolean scarred = yaml.getBoolean(key + ".scarred", false);
            offices.put(key, new FieldOffice(owner, flow, scarred));
        }
        plugin.getLogger().info("Loaded " + offices.size() + " Field Office(s).");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, FieldOffice> e : offices.entrySet()) {
            yaml.set(e.getKey() + ".owner", e.getValue().owner().toString());
            yaml.set(e.getKey() + ".flow", e.getValue().flow());
            yaml.set(e.getKey() + ".scarred", e.getValue().scarred());
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
        offices.put(keyOf(loc), new FieldOffice(owner, 0, false));
        save();
    }

    void flip(Location loc, UUID newOwner) {
        boolean wasScarred = offices.containsKey(keyOf(loc)) && offices.get(keyOf(loc)).scarred();
        offices.put(keyOf(loc), new FieldOffice(newOwner, 0, wasScarred));
        save();
    }

    void tickFlow(long amount) {
        boolean changed = false;
        for (Map.Entry<String, FieldOffice> e : offices.entrySet()) {
            FieldOffice fo = e.getValue();
            long gain = fo.scarred() ? amount / 2 : amount;
            e.setValue(new FieldOffice(fo.owner(), fo.flow() + gain, fo.scarred()));
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

    // VS4: a Field Office that fails Rogue Swarm containment loses its
    // owner (reverts to unclaimed, must be reclaimed like new) and is
    // marked scarred -- halved Flow generation persists across the next
    // claim, per flip()'s own scarred-carries-over behavior above.
    void scar(String foKey) {
        FieldOffice existing = offices.get(foKey);
        if (existing == null) return;
        offices.remove(foKey);
        // Re-inserted as scarred-but-unclaimed via a sentinel: absence from
        // the map means "never claimed" everywhere else in this codebase,
        // so scarred-but-unclaimed needs its own small side table instead
        // of overloading FieldOffice's owner field with a null.
        scarredUnclaimed.add(foKey);
        save();
    }

    private final java.util.Set<String> scarredUnclaimed = new java.util.HashSet<>();

    boolean isScarredUnclaimed(String foKey) {
        return scarredUnclaimed.contains(foKey);
    }

    // Claim path used after a scar wipes ownership -- same as claim() but
    // preserves the scarred flag instead of resetting it, and clears the
    // unclaimed-scar bookkeeping.
    void claimScarred(Location loc, UUID owner) {
        String key = keyOf(loc);
        scarredUnclaimed.remove(key);
        offices.put(key, new FieldOffice(owner, 0, true));
        save();
    }
}
