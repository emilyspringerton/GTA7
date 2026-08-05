package industrial.einhorn.gta7;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

// Per-Field-Office alertness, 0-100. In-memory by design (a "heat" reading,
// not durable state -- a restart means the city calmed down, which is a
// reasonable read of a server bounce). VS1 tracks alertness per FO location
// rather than per hand-authored "district", since no district boundaries
// exist yet on this real world (see NORTHSTAR.md Open Question #2).
final class WatcherManager {

    private static final int MAX = 100;

    private final Map<String, Integer> alertness = new HashMap<>();

    void bump(Location loc, int amount) {
        String key = FieldOfficeManager.keyOf(loc);
        int current = alertness.getOrDefault(key, 0);
        alertness.put(key, Math.min(MAX, current + amount));
    }

    int get(Location loc) {
        return alertness.getOrDefault(FieldOfficeManager.keyOf(loc), 0);
    }

    void decayAll(int amount) {
        alertness.replaceAll((k, v) -> Math.max(0, v - amount));
    }

    Map<String, Integer> snapshot() {
        return Map.copyOf(alertness);
    }
}
