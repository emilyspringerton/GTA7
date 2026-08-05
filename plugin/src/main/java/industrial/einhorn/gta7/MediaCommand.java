package industrial.einhorn.gta7;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// /gta7tv -- spawns a broadcast TextDisplay at the player's location,
// facing however they're standing. No resource pack, no client mod --
// real server-side entity per NORTHSTAR.md's Media section.
final class MediaCommand implements CommandExecutor {

    private final MediaManager media;

    MediaCommand(MediaManager media) {
        this.media = media;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        media.spawnTv(player.getEyeLocation());
        player.sendMessage("§6[GTA7] §fBroadcast TV placed. It'll show recent city activity.");
        return true;
    }
}
