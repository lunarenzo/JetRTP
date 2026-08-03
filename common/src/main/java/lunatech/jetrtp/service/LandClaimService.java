package lunatech.jetrtp.service;

import org.bukkit.Location;

public interface LandClaimService {
    /**
     * Checks if a location is inside any supported claim regions.
     *
     * @param loc the location to check
     * @return true if inside a claim, false otherwise
     */
    boolean isInsideClaim(Location loc);
}
