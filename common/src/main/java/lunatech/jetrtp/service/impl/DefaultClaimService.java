package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.service.LandClaimService;
import org.bukkit.Location;

public class DefaultClaimService implements LandClaimService {
    @Override
    public boolean isInsideClaim(Location loc) {
        return false; // Phase 1: placeholder implementation
    }
}
