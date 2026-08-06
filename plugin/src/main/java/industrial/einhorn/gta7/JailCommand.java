package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// /gta7jail -- sets Custody Lock's real jail location to the sender's
// current position. One real location, set once by whoever's operating
// the server (op permission), same admin-action shape as most Minecraft
// world-building commands.
final class JailCommand implements CommandExecutor {

    private final CustodyManager custody;

    JailCommand(CustodyManager custody) {
        this.custody = custody;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§6[GTA7] §fOnly server operators can set the jail location.");
            return true;
        }
        custody.setJail(player.getLocation());
        player.sendMessage("§6[GTA7] §fCustody Lock jail set to your current location.");
        return true;
    }
}
