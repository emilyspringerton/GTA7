package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class RailroadCommand implements CommandExecutor {

    private final RailroadManager railroad;

    RailroadCommand(RailroadManager railroad) {
        this.railroad = railroad;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        long placed = railroad.railsPlacedBy(player.getUniqueId());
        RailroadManager.Era era = railroad.eraFor(player.getUniqueId());
        RailroadManager.Era next = railroad.nextEraFor(player.getUniqueId());

        player.sendMessage("§6[GTA7 Railroad] §fEra: §e" + era.label + " §f-- rails placed: " + placed);
        if (next != null) {
            long remaining = next.threshold - placed;
            player.sendMessage("§fNext: §e" + next.label + " §fin " + remaining + " more rail(s) (at " + next.threshold + " total).");
        } else {
            player.sendMessage("§fThe network is complete -- every rail type and minecart is yours to build.");
        }
        return true;
    }
}
