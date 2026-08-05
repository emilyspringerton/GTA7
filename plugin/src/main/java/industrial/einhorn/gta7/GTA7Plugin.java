package industrial.einhorn.gta7;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

// VS0: Field Office claim/Flow/Contest Window loop.
// VS1: Watcher alertness + Enforcement spawns.
// VS2: K9 Doctrine (Contest Window defense) + Party Stores.
// VS3: Media broadcast TVs + Factions.
// IDUNA/WOTAN integration: real Apple receipts + shared player identity.
// See docs/NORTHSTAR.md.
public final class GTA7Plugin extends JavaPlugin {

    private static final long FLOW_TICK_PERIOD = 20L * 60;      // every 60s
    private static final long FLOW_PER_TICK = 5;
    private static final long WATCHER_DECAY_PERIOD = 20L * 30;  // every 30s
    private static final int WATCHER_DECAY_AMOUNT = 5;
    private static final long ENFORCEMENT_TICK_PERIOD = 20L * 20; // every 20s
    private static final String IDUNA_BASE_URL = "http://localhost:8080";
    private static final Path IDUNA_SECRETS_FILE = Path.of("/home/fatbaby/IDUNA/var/agent-secrets.env");

    private FieldOfficeManager offices;
    private PartyStoreManager partyStores;
    private FactionManager factions;

    @Override
    public void onEnable() {
        offices = new FieldOfficeManager(this);
        offices.load();
        partyStores = new PartyStoreManager(this);
        partyStores.load();
        factions = new FactionManager(this);
        factions.load();

        IdunaClient iduna = new IdunaClient(this, IDUNA_BASE_URL, IDUNA_SECRETS_FILE);
        if (!iduna.isConfigured()) {
            getLogger().warning("No GTA7-SERVER secret found at " + IDUNA_SECRETS_FILE
                    + " -- receipts/player-identity will be skipped.");
        }

        ContestManager contests = new ContestManager();
        ReceiptPoster receipts = new ReceiptPoster(this, iduna);
        WatcherManager watchers = new WatcherManager();
        MediaManager media = new MediaManager(this);
        EnforcementManager enforcement = new EnforcementManager(this, offices, watchers, media);
        K9Manager k9 = new K9Manager();

        getServer().getPluginManager().registerEvents(
                new FieldOfficeListener(this, offices, contests, receipts, watchers, k9, media, factions), this);
        getServer().getPluginManager().registerEvents(
                new WatcherListener(offices, watchers), this);
        getServer().getPluginManager().registerEvents(
                new PlayerIdentityListener(this, iduna, factions), this);
        getServer().getPluginManager().registerEvents(
                new K9Listener(offices, k9), this);
        getServer().getPluginManager().registerEvents(
                new PartyStoreListener(partyStores, media), this);
        getCommand("flow").setExecutor(new FlowCommand(offices));
        getCommand("faction").setExecutor(new FactionCommand(factions));
        getCommand("gta7tv").setExecutor(new MediaCommand(media));

        getServer().getScheduler().runTaskTimer(this, () -> offices.tickFlow(FLOW_PER_TICK),
                FLOW_TICK_PERIOD, FLOW_TICK_PERIOD);
        getServer().getScheduler().runTaskTimer(this, () -> watchers.decayAll(WATCHER_DECAY_AMOUNT),
                WATCHER_DECAY_PERIOD, WATCHER_DECAY_PERIOD);
        getServer().getScheduler().runTaskTimer(this, enforcement::tick,
                ENFORCEMENT_TICK_PERIOD, ENFORCEMENT_TICK_PERIOD);

        getLogger().info("GTA7 enabled -- VS0/VS1/VS2/VS3 + IDUNA integration live.");
    }

    @Override
    public void onDisable() {
        if (offices != null) offices.save();
        if (partyStores != null) partyStores.save();
        if (factions != null) factions.save();
        getLogger().info("GTA7 disabled.");
    }
}
