package industrial.einhorn.gta7;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// K9 Doctrine (VS2): tamed Wolves assigned to a Field Office defend its
// Contest Windows. Diminishing returns per TRAPX's own 0.85^n spec -- each
// additional dog helps less than the last, so stacking dogs isn't a hard
// counter to a determined contest.
final class K9Manager {

    private static final double DECAY = 0.85;

    private final Map<String, List<UUID>> squads = new HashMap<>();

    void assign(String foKey, UUID wolfId) {
        squads.computeIfAbsent(foKey, k -> new ArrayList<>()).add(wolfId);
    }

    // Live, still-tamed wolf UUIDs for a Field Office. Prunes dead/invalid
    // entries as a side effect, same pattern as EnforcementManager's squads.
    List<UUID> liveUnits(String foKey) {
        List<UUID> squad = squads.get(foKey);
        if (squad == null) return List.of();
        List<UUID> alive = squad.stream()
                .filter(id -> Bukkit.getEntity(id) instanceof org.bukkit.entity.Wolf wolf
                        && wolf.isValid() && wolf.isTamed())
                .collect(Collectors.toList());
        squads.put(foKey, alive);
        return alive;
    }

    // 1 + 0.85 + 0.85^2 + ... for n live units -- diminishing marginal value.
    double defenseScore(String foKey) {
        int n = liveUnits(foKey).size();
        double score = 0;
        double term = 1.0;
        for (int i = 0; i < n; i++) {
            score += term;
            term *= DECAY;
        }
        return score;
    }
}
