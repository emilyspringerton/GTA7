package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// /gta7hospital -- sets CRAZY_KRANKENVAGEN's real hospital delivery point to the sender's
// current position. Same admin-action shape as /gta7jail (JailCommand) -- one real location,
// set once by whoever's operating the server.
final class HospitalCommand implements CommandExecutor {

    private final KrankenvagenManager krankenvagen;

    HospitalCommand(KrankenvagenManager krankenvagen) {
        this.krankenvagen = krankenvagen;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§6[GTA7] §fOnly server operators can set the hospital location.");
            return true;
        }
        krankenvagen.setHospital(player.getLocation());
        player.sendMessage("§6[GTA7] §fCRAZY_KRANKENVAGEN hospital set to your current location.");
        return true;
    }
}
