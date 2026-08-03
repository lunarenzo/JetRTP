package lunatech.jetrtp.service;

import lunatech.jetrtp.config.RtpProfile;
import org.bukkit.Location;
import java.util.concurrent.CompletableFuture;

public interface SafeLocationService {
    /**
     * Asynchronously finds a safe location using non-blocking chunk loading.
     *
     * @param profile the RTP settings profile to respect
     * @param center the center location to search from
     * @return a future that completes with the safe location
     */
    CompletableFuture<Location> findSafeLocationAsync(RtpProfile profile, Location center);
}
