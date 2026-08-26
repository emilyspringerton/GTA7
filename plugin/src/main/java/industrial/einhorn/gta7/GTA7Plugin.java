package industrial.einhorn.gta7;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

// VS0: Field Office claim/Flow/Contest Window loop.
// VS1: Watcher alertness + Enforcement spawns.
// VS2: K9 Doctrine (Contest Window defense) + Party Stores.
// VS3: Media broadcast TVs + Factions.
// VS4: Rogue Swarms (containment event, district scar) + Custody Lock.
// VS5: CRAZY_KRANKENVAGEN -- ambulance-boat paramedic runs (KrankenvagenManager).
// VS6: Historical railroad tech tree -- real rails placed unlocks real rail/minecart types (RailroadManager).
// IDUNA/WOTAN integration: real Apple receipts + shared player identity.
// S171-04: GFD <-> EINHORN_SURVIVAL chat bridge (EINHORN_SURVIVAL side).
// See docs/NORTHSTAR.md.
public final class GTA7Plugin extends JavaPlugin {

    private static final long FLOW_TICK_PERIOD = 20L * 60;      // every 60s
    private static final long FLOW_PER_TICK = 5;
    private static final long WATCHER_DECAY_PERIOD = 20L * 30;  // every 30s
    private static final int WATCHER_DECAY_AMOUNT = 5;
    private static final long ENFORCEMENT_TICK_PERIOD = 20L * 20; // every 20s
    private static final long ROGUE_SWARM_TICK_PERIOD = 20L * 15; // every 15s
    private static final long CUSTODY_TICK_PERIOD = 20L * 5;      // every 5s
    private static final long CHAT_BRIDGE_TICK_PERIOD = 20L * 5;  // every 5s
    private static final long KRANKENVAGEN_TICK_PERIOD = 20L * 5; // every 5s -- timer expiry check + wounded-NPC spawn
    private static final String IDUNA_BASE_URL = "http://localhost:8080";
    private static final Path IDUNA_SECRETS_FILE = Path.of("/home/fatbaby/IDUNA/var/agent-secrets.env");

    private FieldOfficeManager offices;
    private PartyStoreManager partyStores;
    private FactionManager factions;
    private CustodyManager custody;
    private KrankenvagenManager krankenvagen;
    private RailroadManager railroad;

    @Override
    public void onEnable() {
        offices = new FieldOfficeManager(this);
        offices.load();
        partyStores = new PartyStoreManager(this);
        partyStores.load();
        factions = new FactionManager(this);
        factions.load();
        custody = new CustodyManager(this);
        custody.load();
        railroad = new RailroadManager(this);
        railroad.load();

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
        RogueSwarmManager rogueSwarms = new RogueSwarmManager(this, offices, watchers, media, factions);
        krankenvagen = new KrankenvagenManager(this, offices, factions, media);
        krankenvagen.load();

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
        getServer().getPluginManager().registerEvents(
                new RogueSwarmListener(rogueSwarms), this);
        getServer().getPluginManager().registerEvents(
                new CustodyListener(custody), this);
        getServer().getPluginManager().registerEvents(
                new ChatBridgeListener(iduna), this);
        getServer().getPluginManager().registerEvents(
                new KrankenvagenListener(krankenvagen), this);
        getServer().getPluginManager().registerEvents(
                new RespawnGearListener(), this);
        getServer().getPluginManager().registerEvents(
                new RailroadListener(railroad), this);
        getCommand("flow").setExecutor(new FlowCommand(offices));
        getCommand("faction").setExecutor(new FactionCommand(factions));
        getCommand("gta7tv").setExecutor(new MediaCommand(media));
        getCommand("gta7jail").setExecutor(new JailCommand(custody));
        getCommand("gta7hospital").setExecutor(new HospitalCommand(krankenvagen));
        getCommand("sudoku").setExecutor(new SudokuCommand());
        getCommand("railroad").setExecutor(new RailroadCommand(railroad));

        getServer().getScheduler().runTaskTimer(this, () -> offices.tickFlow(FLOW_PER_TICK),
                FLOW_TICK_PERIOD, FLOW_TICK_PERIOD);
        getServer().getScheduler().runTaskTimer(this, () -> watchers.decayAll(WATCHER_DECAY_AMOUNT),
                WATCHER_DECAY_PERIOD, WATCHER_DECAY_PERIOD);
        getServer().getScheduler().runTaskTimer(this, enforcement::tick,
                ENFORCEMENT_TICK_PERIOD, ENFORCEMENT_TICK_PERIOD);
        getServer().getScheduler().runTaskTimer(this, rogueSwarms::tick,
                ROGUE_SWARM_TICK_PERIOD, ROGUE_SWARM_TICK_PERIOD);
        getServer().getScheduler().runTaskTimer(this, custody::tick,
                CUSTODY_TICK_PERIOD, CUSTODY_TICK_PERIOD);
        getServer().getScheduler().runTaskTimer(this, krankenvagen::tick,
                KRANKENVAGEN_TICK_PERIOD, KRANKENVAGEN_TICK_PERIOD);

        ChatBridgePoller chatBridge = new ChatBridgePoller(this, iduna);
        getServer().getScheduler().runTaskTimerAsynchronously(this, chatBridge::tick,
                CHAT_BRIDGE_TICK_PERIOD, CHAT_BRIDGE_TICK_PERIOD);

        getLogger().info("GTA7 enabled -- VS0/VS1/VS2/VS3/VS4/VS5 + IDUNA integration + chat bridge live.");
    }

    @Override
    public void onDisable() {
        if (offices != null) offices.save();
        if (partyStores != null) partyStores.save();
        if (factions != null) factions.save();
        if (custody != null) custody.save();
        if (krankenvagen != null) krankenvagen.save();
        if (railroad != null) railroad.save();
        getLogger().info("GTA7 disabled.");
    }
}
