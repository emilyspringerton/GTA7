package industrial.einhorn.gta7;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

// VS0 Field Office loop: right-click a Beacon to claim it if unheld, or open
// a Contest Window against the current owner if it's already held.
// VS2: K9 units assigned to the Field Office extend the Contest Window
// (diminishing returns, K9Manager.defenseScore) and are set on the
// challenger for its duration -- "a K9 unit meaningfully slows a Contest
// Window flip attempt" per NORTHSTAR.md VS2's own acceptance criterion.
final class FieldOfficeListener implements Listener {

    private static final Material MARKER = Material.BEACON;
    private static final long CONTEST_TICKS_BASE = 20L * 60; // 60s
    private static final long CONTEST_TICKS_PER_DEFENSE = 20L * 10; // +10s per defense-score point
    private static final double CONTEST_RADIUS = 15.0;

    private static final int REP_PER_CLAIM = 5;
    private static final int REP_PER_FLIP = 10;

    private final JavaPlugin plugin;
    private final FieldOfficeManager offices;
    private final ContestManager contests;
    private final ReceiptPoster receipts;
    private final WatcherManager watchers;
    private final K9Manager k9;
    private final MediaManager media;
    private final FactionManager factions;

    FieldOfficeListener(JavaPlugin plugin, FieldOfficeManager offices, ContestManager contests,
                         ReceiptPoster receipts, WatcherManager watchers, K9Manager k9,
                         MediaManager media, FactionManager factions) {
        this.plugin = plugin;
        this.offices = offices;
        this.contests = contests;
        this.receipts = receipts;
        this.watchers = watchers;
        this.k9 = k9;
        this.media = media;
        this.factions = factions;
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
        if (offices.isScarredUnclaimed(FieldOfficeManager.keyOf(loc))) {
            offices.claimScarred(loc, player.getUniqueId());
            player.sendMessage("§7[GTA7] §fThis Field Office bears a scar from a Rogue Swarm -- Flow generation is halved here.");
        } else {
            offices.claim(loc, player.getUniqueId());
        }
        watchers.bump(loc, 15);
        factions.addRep(player.getUniqueId(), REP_PER_CLAIM);
        String line = player.getName() + " claimed a Field Office at " + fmt(loc) + ".";
        Bukkit.broadcastMessage("§6[GTA7] §f" + line);
        media.broadcast(line);
        receipts.post("Field Office claimed at " + fmt(loc),
                player.getName() + " (" + player.getUniqueId() + ") claimed an unheld Field Office.");
    }

    private void openContest(Player challenger, Location loc, UUID ownerId) {
        contests.start(loc, challenger.getUniqueId());
        watchers.bump(loc, 25);

        String foKey = FieldOfficeManager.keyOf(loc);
        double defense = k9.defenseScore(foKey);
        long ticks = CONTEST_TICKS_BASE + Math.round(defense * CONTEST_TICKS_PER_DEFENSE);
        for (UUID wolfId : k9.liveUnits(foKey)) {
            if (Bukkit.getEntity(wolfId) instanceof Mob mob) {
                mob.setTarget(challenger);
            }
        }

        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        String k9Note = defense > 0 ? " (K9 units engaged, +" + (ticks - CONTEST_TICKS_BASE) / 20 + "s)" : "";
        String line = "Contest Window opened at " + fmt(loc) + " -- " + challenger.getName()
                + " vs " + ownerName + "." + k9Note;
        Bukkit.broadcastMessage("§c[GTA7] §f" + line + " " + (ticks / 20) + " seconds.");
        media.broadcast(line);

        Bukkit.getScheduler().runTaskLater(plugin, () -> resolveContest(loc, challenger.getUniqueId(), ownerId), ticks);
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
            factions.addRep(challengerId, REP_PER_FLIP);
            String line = challenger.getName() + " flipped a Field Office at " + fmt(loc) + "!";
            Bukkit.broadcastMessage("§a[GTA7] §f" + line);
            media.broadcast(line);
            receipts.post("Field Office flipped at " + fmt(loc),
                    challenger.getName() + " (" + challengerId + ") won a Contest Window against "
                            + Bukkit.getOfflinePlayer(ownerId).getName() + ".");
        } else {
            String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
            String line = "Contest Window at " + fmt(loc) + " failed -- " + ownerName + " holds.";
            Bukkit.broadcastMessage("§7[GTA7] §f" + line);
            media.broadcast(line);
        }
    }

    private static String fmt(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
