package industrial.einhorn.gta7;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Party Stores (VS2): a designated Villager with real day/night hours and
// merchant memory (who keeps them solvent vs. brings trouble), per
// SHANKPIT/docs2/PARTY_STORES.md. VS2 slice: hours + trouble-triggered early
// closing. Merchant "who trusts whom" nuance beyond a flat goodwill counter
// is future work, not attempted here.
final class PartyStoreManager {

    static final class Store {
        final Map<UUID, Integer> goodwill = new HashMap<>();
        long forcedClosedUntilMillis = 0;
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Store> stores = new HashMap<>();

    PartyStoreManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "party-stores.yml");
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String villagerId : yaml.getKeys(false)) {
            Store store = new Store();
            store.forcedClosedUntilMillis = yaml.getLong(villagerId + ".closedUntil");
            if (yaml.contains(villagerId + ".goodwill")) {
                for (String playerId : yaml.getConfigurationSection(villagerId + ".goodwill").getKeys(false)) {
                    store.goodwill.put(UUID.fromString(playerId), yaml.getInt(villagerId + ".goodwill." + playerId));
                }
            }
            stores.put(villagerId, store);
        }
        plugin.getLogger().info("Loaded " + stores.size() + " Party Store(s).");
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Store> e : stores.entrySet()) {
            yaml.set(e.getKey() + ".closedUntil", e.getValue().forcedClosedUntilMillis);
            for (Map.Entry<UUID, Integer> gw : e.getValue().goodwill.entrySet()) {
                yaml.set(e.getKey() + ".goodwill." + gw.getKey(), gw.getValue());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save party-stores.yml: " + ex.getMessage());
        }
    }

    boolean isStore(UUID villagerId) {
        return stores.containsKey(villagerId.toString());
    }

    void designate(UUID villagerId) {
        stores.put(villagerId.toString(), new Store());
        save();
    }

    Store get(UUID villagerId) {
        return stores.get(villagerId.toString());
    }

    void recordTrade(UUID villagerId, UUID playerId) {
        Store store = get(villagerId);
        if (store == null) return;
        store.goodwill.merge(playerId, 1, Integer::sum);
        save();
    }

    void recordTrouble(UUID villagerId, long closedForMillis) {
        Store store = get(villagerId);
        if (store == null) return;
        store.forcedClosedUntilMillis = Math.max(store.forcedClosedUntilMillis, System.currentTimeMillis() + closedForMillis);
        save();
    }

    boolean isForcedClosed(UUID villagerId) {
        Store store = get(villagerId);
        return store != null && System.currentTimeMillis() < store.forcedClosedUntilMillis;
    }
}
