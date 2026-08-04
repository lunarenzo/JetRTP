package lunatech.jetrtp.service;

import lunatech.jetrtp.config.RtpProfile;
import org.bukkit.Location;

public interface LocationCacheService {
    /**
     * Pops a cached safe location from the profile's queue.
     *
     * @param profile the RTP profile
     * @return the safe location, or null if the queue is empty
     */
    Location popCachedLocation(RtpProfile profile);

    /**
     * Asynchronously refills the cache for the profile if it is below target size.
     *
     * @param profile the RTP profile
     */
    void refillCacheAsync(RtpProfile profile);

    /**
     * Gets the current number of cached locations for the profile.
     *
     * @param profile the RTP profile
     * @return cache size
     */
    int getCacheSize(RtpProfile profile);

    /**
     * Gets the number of cache hits for the profile.
     */
    long getHits(RtpProfile profile);

    /**
     * Gets the number of cache misses for the profile.
     */
    long getMisses(RtpProfile profile);

    /**
     * Gets the average search duration in milliseconds for the profile.
     */
    double getAverageSearchTime(RtpProfile profile);

    /**
     * Records a cache hit.
     */
    void recordHit(RtpProfile profile);

    /**
     * Records a cache miss.
     */
    void recordMiss(RtpProfile profile);

    /**
     * Records a safe location search time duration.
     */
    void recordSearchTime(RtpProfile profile, long durationMs);

    /**
     * Starts background tasks to monitor and reload caches.
     */
    void startRefillTask();

    /**
     * Clears all cached queues.
     */
    void shutdown();
}
