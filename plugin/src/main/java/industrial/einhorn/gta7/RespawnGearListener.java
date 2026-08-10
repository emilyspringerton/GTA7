package industrial.einhorn.gta7;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

// Founder real-time, 2026-08-10 (GTA7 Enforcement-squad difficulty thread -- "ok HTA7 this shit
// is hard" -> Enforcement squads named as the specific pain point -> "can i please always
// respawn with wooden sword" -> "all players" -> "always"): every player always respawns with at
// least one wooden sword, so a fresh respawn (often right after an Enforcement death -- see
// EnforcementManager's own doc comment) isn't bare-fisted against a squad that's still out there.
// Not a full loadout -- just enough to fight back, matching this system's own "real but fair
// threat" framing rather than removing the threat entirely.
final class RespawnGearListener implements Listener {

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().contains(Material.WOODEN_SWORD)) return;
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD, 1));
    }
}
