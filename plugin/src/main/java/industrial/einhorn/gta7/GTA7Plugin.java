package industrial.einhorn.gta7;

import org.bukkit.plugin.java.JavaPlugin;

// VS0 Field Office claim/Flow/Contest Window loop is the first real system
// to land here — see docs/NORTHSTAR.md. This is the loadable skeleton only.
public final class GTA7Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("GTA7 enabled — TRAPX doctrine on real Minecraft. See docs/NORTHSTAR.md.");
    }

    @Override
    public void onDisable() {
        getLogger().info("GTA7 disabled.");
    }
}
