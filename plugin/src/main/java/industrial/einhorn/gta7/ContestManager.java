package industrial.einhorn.gta7;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// In-memory only, on purpose: a contest mid-flight when the server restarts
// is meant to just be lost (challenger has to re-initiate), not resumed with
// stale state. Field Office ownership itself (FieldOfficeManager) is the
// thing that's durable -- an active contest is not.
final class ContestManager {

    record Contest(UUID challenger, long startedAtMillis) {}

    private final Map<String, Contest> active = new HashMap<>();

    boolean isActive(Location loc) {
        return active.containsKey(FieldOfficeManager.keyOf(loc));
    }

    Contest get(Location loc) {
        return active.get(FieldOfficeManager.keyOf(loc));
    }

    void start(Location loc, UUID challenger) {
        active.put(FieldOfficeManager.keyOf(loc), new Contest(challenger, System.currentTimeMillis()));
    }

    void end(Location loc) {
        active.remove(FieldOfficeManager.keyOf(loc));
    }
}
