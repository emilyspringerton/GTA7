package industrial.einhorn.gta7;

import org.bukkit.plugin.java.JavaPlugin;

// VS0: Field Office claim/Flow/Contest Window loop.
// VS1: Watcher alertness + Enforcement spawns.
// See docs/NORTHSTAR.md.
public final class GTA7Plugin extends JavaPlugin {

    private static final long FLOW_TICK_PERIOD = 20L * 60;      // every 60s
    private static final long FLOW_PER_TICK = 5;
    private static final long WATCHER_DECAY_PERIOD = 20L * 30;  // every 30s
    private static final int WATCHER_DECAY_AMOUNT = 5;
    private static final long ENFORCEMENT_TICK_PERIOD = 20L * 20; // every 20s

    private FieldOfficeManager offices;

    @Override
    public void onEnable() {
        offices = new FieldOfficeManager(this);
        offices.load();

        ContestManager contests = new ContestManager();
        ReceiptPoster receipts = new ReceiptPoster(this);
        WatcherManager watchers = new WatcherManager();
        EnforcementManager enforcement = new EnforcementManager(this, offices, watchers);

        getServer().getPluginManager().registerEvents(
                new FieldOfficeListener(this, offices, contests, receipts, watchers), this);
        getServer().getPluginManager().registerEvents(
                new WatcherListener(offices, watchers), this);
        getCommand("flow").setExecutor(new FlowCommand(offices));

        getServer().getScheduler().runTaskTimer(this, () -> offices.tickFlow(FLOW_PER_TICK),
                FLOW_TICK_PERIOD, FLOW_TICK_PERIOD);
        getServer().getScheduler().runTaskTimer(this, () -> watchers.decayAll(WATCHER_DECAY_AMOUNT),
                WATCHER_DECAY_PERIOD, WATCHER_DECAY_PERIOD);
        getServer().getScheduler().runTaskTimer(this, enforcement::tick,
                ENFORCEMENT_TICK_PERIOD, ENFORCEMENT_TICK_PERIOD);

        getLogger().info("GTA7 enabled -- VS0 Field Office loop + VS1 Watchers/Enforcement live.");
    }

    @Override
    public void onDisable() {
        if (offices != null) offices.save();
        getLogger().info("GTA7 disabled.");
    }
}
