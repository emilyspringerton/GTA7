package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

// VS0 Field Office loop: right-click a Beacon to claim it if unheld, or open
// a Contest Window against the current owner if it's already held. See
// GTA7/docs/NORTHSTAR.md VS0 for the full spec this implements.
final class FieldOfficeListener implements Listener {

    private static final Material MARKER = Material.BEACON;
    private static final long CONTEST_TICKS = 20L * 60; // 60s
    private static final double CONTEST_RADIUS = 15.0;

    private final JavaPlugin plugin;
    private final FieldOfficeManager offices;
    private final ContestManager contests;
    private final ReceiptPoster receipts;
    private final WatcherManager watchers;

    FieldOfficeListener(JavaPlugin plugin, FieldOfficeManager offices, ContestManager contests,
                         ReceiptPoster receipts, WatcherManager watchers) {
        this.plugin = plugin;
        this.offices = offices;
        this.contests = contests;
        this.receipts = receipts;
        this.watchers = watchers;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != MARKER) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        FieldOfficeManager.FieldOffice existing = offices.get(loc);

        if (existing == null) {
            claim(player, loc);
            return;
        }
        if (existing.owner().equals(player.getUniqueId())) {
            player.sendMessage("§6[GTA7] §fYou hold this Field Office. Flow: " + existing.flow());
            return;
        }
        if (contests.isActive(loc)) {
            player.sendMessage("§6[GTA7] §fA Contest Window is already open here.");
            return;
        }
        openContest(player, loc, existing.owner());
    }

    private void claim(Player player, Location loc) {
        offices.claim(loc, player.getUniqueId());
        watchers.bump(loc, 15);
        Bukkit.broadcastMessage("§6[GTA7] §f" + player.getName() + " claimed a Field Office at "
                + fmt(loc) + ".");
        receipts.post("Field Office claimed at " + fmt(loc),
                player.getName() + " (" + player.getUniqueId() + ") claimed an unheld Field Office.");
    }

    private void openContest(Player challenger, Location loc, UUID ownerId) {
        contests.start(loc, challenger.getUniqueId());
        watchers.bump(loc, 25);
        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        Bukkit.broadcastMessage("§c[GTA7] §fContest Window opened at " + fmt(loc) + " -- "
                + challenger.getName() + " vs " + ownerName + ". 60 seconds.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> resolveContest(loc, challenger.getUniqueId(), ownerId), CONTEST_TICKS);
    }

    private void resolveContest(Location loc, UUID challengerId, UUID ownerId) {
        contests.end(loc);
        Player challenger = Bukkit.getPlayer(challengerId);
        boolean flips = challenger != null
                && challenger.isOnline()
                && challenger.getWorld().equals(loc.getWorld())
                && challenger.getLocation().distance(loc) <= CONTEST_RADIUS;

        if (flips) {
            offices.flip(loc, challengerId);
            watchers.bump(loc, 15);
            Bukkit.broadcastMessage("§a[GTA7] §fField Office at " + fmt(loc) + " flipped to "
                    + challenger.getName() + "!");
            receipts.post("Field Office flipped at " + fmt(loc),
                    challenger.getName() + " (" + challengerId + ") won a Contest Window against "
                            + Bukkit.getOfflinePlayer(ownerId).getName() + ".");
        } else {
            String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
            Bukkit.broadcastMessage("§7[GTA7] §fContest Window at " + fmt(loc) + " failed -- "
                    + ownerName + " holds.");
        }
    }

    private static String fmt(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
