package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// /sudoku -- self-KO -> respawn, for a player stuck in terrain (a hole,
// a void gap, wedged geometry) with no other way out. Founder, real-time
// (2026-08-09): "/sudoku ... should do a self smite causing a KO allowing
// respawn." setHealth(0.0) is used rather than real lightning damage --
// lightning strikes are armor/absorption-dependent and not reliably
// lethal, which would defeat the point of a guaranteed unstick tool.
// strikeLightningEffect is the purely cosmetic (no-damage) variant, giving
// the "smite" flavor without risking a non-lethal strike. No killer
// LivingEntity is involved, so CustodyListener.onDeath's Enforcement-kill
// check never fires -- self-sudoku does not send the player to Custody
// Lock jail.
final class SudokuCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        player.getWorld().strikeLightningEffect(player.getLocation());
        player.setHealth(0.0);
        player.sendMessage("§6[GTA7] §fSelf-smited. Respawn and try again.");
        return true;
    }
}
