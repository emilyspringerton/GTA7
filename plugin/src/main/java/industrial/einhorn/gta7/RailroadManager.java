package industrial.einhorn.gta7;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// VS6: historical railroad tech tree. Founder real-time: "can we build out
// the gta7 historical based railroad tech tree powered by real minecraft
// rails." Progression currency is deliberately NOT Flow (GTA7's existing
// currency is Field-Office-owned, not a per-player spendable wallet --
// FlowCommand only ever displays it, nothing spends it yet; building a
// whole new spend-from-FO mechanic just for this would be real, separate
// scope). Instead: real rails placed, tracked per player, is the honest
// "historical progression" metric this ask's own "powered by real
// minecraft rails" phrasing already points at directly -- you advance the
// tech tree by literally building more railroad, no abstract currency
// needed.
//
// Persistence mirrors FieldOfficeManager's own flat-YAML pattern exactly
// (one file in the plugin's data folder, load-all-at-boot/save-on-write).
final class RailroadManager {

    // Five real historical rail eras, each gated by a real cumulative
    // rails-placed threshold. Thresholds are a real, honest guess (this
    // session's own "founder specifies the trigger, reasonable numbers
    // fill the rest" convention, same as Bloodflower/Tree-passive's own
    // numbers elsewhere in this monorepo) -- not asked for specifically,
    // flagged rather than silently invented as gospel.
    enum Era {
        WOODEN_TRAMWAY("Wooden Tramway", 0),
        IRON_RAIL("Iron Rail", 50),
        SIGNAL_ERA("Signal Era", 150),
        INDUSTRIAL_RAIL("Industrial Rail", 400),
        HIGH_SPEED_RAIL("High-Speed Rail", 1000);

        final String label;
        final long threshold;

        Era(String label, long threshold) {
            this.label = label;
            this.threshold = threshold;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Long> railsPlaced = new HashMap<>();

    RailroadManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "railroad.yml");
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                railsPlaced.put(UUID.fromString(key), yaml.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // a corrupt/foreign key in the file -- skip rather than fail the whole load
            }
        }
        plugin.getLogger().info("Loaded railroad progress for " + railsPlaced.size() + " player(s).");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : railsPlaced.entrySet()) {
            yaml.set(e.getKey().toString(), e.getValue());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save railroad.yml: " + ex.getMessage());
        }
    }

    long railsPlacedBy(UUID player) {
        return railsPlaced.getOrDefault(player, 0L);
    }

    // Called on every real RAIL block a player places (RailroadListener) --
    // rail placement itself is never gated by era (Wooden Tramway, era 0,
    // already allows plain rails), so every placement counts toward
    // advancing regardless of current era.
    void recordRailPlaced(UUID player) {
        railsPlaced.merge(player, 1L, Long::sum);
        save();
    }

    Era eraFor(UUID player) {
        long placed = railsPlacedBy(player);
        Era current = Era.WOODEN_TRAMWAY;
        for (Era era : Era.values()) {
            if (placed >= era.threshold) current = era;
        }
        return current;
    }

    // null once at the final era -- nothing further to unlock.
    Era nextEraFor(UUID player) {
        Era current = eraFor(player);
        Era[] all = Era.values();
        int idx = current.ordinal();
        return idx + 1 < all.length ? all[idx + 1] : null;
    }

    boolean hasUnlockedPoweredRail(UUID player) {
        return eraFor(player).ordinal() >= Era.IRON_RAIL.ordinal();
    }

    boolean hasUnlockedDetectorRail(UUID player) {
        return eraFor(player).ordinal() >= Era.SIGNAL_ERA.ordinal();
    }

    boolean hasUnlockedActivatorRailAndUtilityCarts(UUID player) {
        return eraFor(player).ordinal() >= Era.INDUSTRIAL_RAIL.ordinal();
    }

    boolean hasUnlockedTntCart(UUID player) {
        return eraFor(player).ordinal() >= Era.HIGH_SPEED_RAIL.ordinal();
    }
}
