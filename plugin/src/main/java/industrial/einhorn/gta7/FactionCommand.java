package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class FactionCommand implements CommandExecutor {

    private final FactionManager factions;

    FactionCommand(FactionManager factions) {
        this.factions = factions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            FactionManager.Faction faction = FactionManager.Faction.parse(args[1]);
            if (faction == null) {
                player.sendMessage("§6[GTA7] §fUnknown faction. Options: FREQUENCY, BLOC, PROCUREMENT.");
                return true;
            }
            factions.join(player, faction);
            player.sendMessage("§6[GTA7] §fYou joined " + faction.color + faction.displayName + "§f.");
            return true;
        }

        FactionManager.Faction current = factions.get(player.getUniqueId());
        if (current == null) {
            player.sendMessage("§6[GTA7] §fNo faction. Use /faction join <FREQUENCY|BLOC|PROCUREMENT>.");
        } else {
            player.sendMessage("§6[GTA7] §fFaction: " + current.color + current.displayName
                    + " §f-- reputation: " + factions.reputation(player.getUniqueId()));
        }
        return true;
    }
}
