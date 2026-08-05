package industrial.einhorn.gta7;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashSet;
import java.util.Set;

// Party Stores (VS2): sneak + right-click an un-designated Villager to make
// it a Party Store. Real trading (vanilla, unmodified) builds merchant
// memory; sustained PvP nearby forces an early close, same "quiet after
// dark" and "closes when it gets loud" spirit as PARTY_STORES.md.
final class PartyStoreListener implements Listener {

    private static final long NIGHT_START = 13000;
    private static final long NIGHT_END = 23000;
    private static final double TROUBLE_RADIUS = 15.0;
    private static final long TROUBLE_CLOSE_MILLIS = 5L * 60 * 1000; // 5 minutes

    private final PartyStoreManager stores;
    private final MediaManager media;

    PartyStoreListener(PartyStoreManager stores, MediaManager media) {
        this.stores = stores;
        this.media = media;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();

        if (!stores.isStore(villager.getUniqueId())) {
            if (!player.isSneaking()) return; // normal trade interaction, not our concern
            event.setCancelled(true);
            stores.designate(villager.getUniqueId());
            villager.setCustomName("§aParty Store");
            villager.setCustomNameVisible(true);
            player.sendMessage("§6[GTA7] §fParty Store designated.");
            return;
        }

        if (player.isSneaking()) {
            event.setCancelled(true);
            PartyStoreManager.Store store = stores.get(villager.getUniqueId());
            int goodwill = store.goodwill.getOrDefault(player.getUniqueId(), 0);
            String status = isOpen(villager) ? "§aOPEN" : "§cCLOSED";
            player.sendMessage("§6[GTA7] §fParty Store: " + status + " §f-- your goodwill: " + goodwill);
            return;
        }

        if (!isOpen(villager)) {
            event.setCancelled(true);
            player.sendMessage("§6[GTA7] §fThis Party Store is closed right now.");
        }
    }

    @EventHandler
    public void onTrade(PlayerTradeEvent event) {
        if (!(event.getVillager() instanceof Villager villager)) return;
        if (!stores.isStore(villager.getUniqueId())) return;
        stores.recordTrade(villager.getUniqueId(), event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Set<Entity> nearby = new HashSet<>(event.getEntity().getNearbyEntities(TROUBLE_RADIUS, TROUBLE_RADIUS, TROUBLE_RADIUS));
        nearby.addAll(event.getDamager().getNearbyEntities(TROUBLE_RADIUS, TROUBLE_RADIUS, TROUBLE_RADIUS));

        for (Entity e : nearby) {
            if (e instanceof Villager villager && stores.isStore(villager.getUniqueId())) {
                boolean wasOpen = isOpen(villager);
                stores.recordTrouble(villager.getUniqueId(), TROUBLE_CLOSE_MILLIS);
                if (wasOpen) {
                    Bukkit.broadcastMessage("§7[GTA7] §fA Party Store closed early -- trouble nearby.");
                    media.broadcast("A Party Store closed early -- trouble nearby.");
                }
            }
        }
    }

    private boolean isOpen(Villager villager) {
        if (stores.isForcedClosed(villager.getUniqueId())) return false;
        long time = villager.getWorld().getTime();
        return time < NIGHT_START || time > NIGHT_END;
    }
}
