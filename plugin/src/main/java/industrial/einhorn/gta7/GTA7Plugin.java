package industrial.einhorn.gta7;

import org.bukkit.plugin.java.JavaPlugin;

// VS0: Field Office claim/Flow/Contest Window loop. See docs/NORTHSTAR.md.
public final class GTA7Plugin extends JavaPlugin {

    private static final long FLOW_TICK_PERIOD = 20L * 60; // every 60s
    private static final long FLOW_PER_TICK = 5;

    private FieldOfficeManager offices;

    @Override
    public void onEnable() {
        offices = new FieldOfficeManager(this);
        offices.load();

        ContestManager contests = new ContestManager();
        ReceiptPoster receipts = new ReceiptPoster(this);

        getServer().getPluginManager().registerEvents(
                new FieldOfficeListener(this, offices, contests, receipts), this);
        getCommand("flow").setExecutor(new FlowCommand(offices));

        getServer().getScheduler().runTaskTimer(this, () -> offices.tickFlow(FLOW_PER_TICK), FLOW_TICK_PERIOD, FLOW_TICK_PERIOD);

        getLogger().info("GTA7 enabled -- VS0 Field Office loop live. Right-click a Beacon to claim it.");
    }

    @Override
    public void onDisable() {
        if (offices != null) offices.save();
        getLogger().info("GTA7 disabled.");
    }
}
