package lunatech.jetrtp.hook.claims;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.service.LandClaimService;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;

public class PaperClaimService implements LandClaimService {
    private final List<ClaimHook> hooks = new ArrayList<>();

    public PaperClaimService(AbstractJetRTP plugin) {
        var pm = plugin.getServer().getPluginManager();
        var cfg = plugin.getConfigHandler().getConfig().landClaimSupport;

        if (!cfg.forceDisableAll) {
            if (cfg.griefPrevention && pm.isPluginEnabled("GriefPrevention")) {
                try {
                    hooks.add(new GriefPreventionHook());
                    plugin.getComponentLogger().info("Loaded GriefPrevention claim integration.");
                } catch (Throwable t) {
                    plugin.getComponentLogger().warn("Failed to load GriefPrevention integration", t);
                }
            }
            if (cfg.worldGuard && pm.isPluginEnabled("WorldGuard")) {
                try {
                    hooks.add(new WorldGuardHook());
                    plugin.getComponentLogger().info("Loaded WorldGuard claim integration.");
                } catch (Throwable t) {
                    plugin.getComponentLogger().warn("Failed to load WorldGuard integration", t);
                }
            }
        }
    }

    @Override
    public boolean isInsideClaim(Location loc) {
        for (ClaimHook hook : hooks) {
            if (hook.isInsideClaim(loc)) {
                return true;
            }
        }
        return false;
    }
}
