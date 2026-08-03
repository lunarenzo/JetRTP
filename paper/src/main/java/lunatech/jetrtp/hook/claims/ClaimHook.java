package lunatech.jetrtp.hook.claims;

import org.bukkit.Location;

public interface ClaimHook {
    /**
     * Checks if the location is inside a claim managed by this hook.
     *
     * @param loc the location to check
     * @return true if inside a claim, false otherwise
     */
    boolean isInsideClaim(Location loc);
}
