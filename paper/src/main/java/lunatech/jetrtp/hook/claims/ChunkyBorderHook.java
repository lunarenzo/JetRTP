package lunatech.jetrtp.hook.claims;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunky.shape.Shape;

public class ChunkyBorderHook implements ClaimHook {

    @Override
    public boolean isInsideClaim(Location loc) {
        try {
            ChunkyBorder cb = Bukkit.getServicesManager().load(ChunkyBorder.class);
            if (cb != null) {
                BorderData borderData = cb.getBorders().get(loc.getWorld().getName());
                if (borderData != null) {
                    Shape shape = borderData.getBorder();
                    if (shape != null) {
                        // Restricted (treated as inside a claim) if it is OUTSIDE the chunky border
                        return !shape.isBounding(loc.getX(), loc.getZ());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
