package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class FlowCommand implements CommandExecutor {

    private final FieldOfficeManager offices;

    FlowCommand(FieldOfficeManager offices) {
        this.offices = offices;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        long flow = offices.totalFlowFor(player.getUniqueId());
        int held = offices.countOwnedBy(player.getUniqueId());
        player.sendMessage("§6[GTA7] §fFlow: " + flow + " -- Field Offices held: " + held);
        return true;
    }
}
