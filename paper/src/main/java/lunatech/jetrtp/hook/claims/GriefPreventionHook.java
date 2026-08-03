package lunatech.jetrtp.hook.claims;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;

public class GriefPreventionHook implements ClaimHook {
    @Override
    public boolean isInsideClaim(Location loc) {
        return GriefPrevention.instance.dataStore.getClaimAt(loc, true, null) != null;
    }
}
