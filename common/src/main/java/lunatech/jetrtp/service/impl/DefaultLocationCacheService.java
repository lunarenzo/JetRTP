package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.model.CachedLocation;
import lunatech.jetrtp.service.LocationCacheService;
import lunatech.jetrtp.service.SafeLocationService;
import lunatech.jetrtp.service.LagService;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.Map;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DefaultLocationCacheService implements LocationCacheService {

    private final AbstractJetRTP plugin;
    private final SafeLocationService safeLocationService;
    private final LagService lagService;
    private final Map<String, Queue<CachedLocation>> cacheMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> refillingMap = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.LongAdder> hitMap = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.LongAdder> missMap = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.LongAdder> totalSearchTimeMap = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.LongAdder> searchCountMap = new ConcurrentHashMap<>();

    public DefaultLocationCacheService(AbstractJetRTP plugin, SafeLocationService safeLocationService, LagService lagService) {
        this.plugin = plugin;
        this.safeLocationService = safeLocationService;
        this.lagService = lagService;
    }

    @Override
    public Location popCachedLocation(RtpProfile profile) {
        Queue<CachedLocation> queue = cacheMap.get(profile.name.toLowerCase());
        if (queue == null) {
            return null;
        }
        CachedLocation cached = queue.poll();
        if (cached != null) {
            // Trigger refill in the background
            refillCacheAsync(profile);

            World world = plugin.getServer().getWorld(cached.worldName());
            if (world != null) {
                return new Location(world, cached.x(), cached.y(), cached.z(), cached.yaw(), cached.pitch());
            }
        }
        return null;
    }

    @Override
    public void refillCacheAsync(RtpProfile profile) {
        if (!profile.enabled || profile.preparations.cacheLocations <= 0) {
            return;
        }

        if (lagService.isLagging()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> refillCacheAsync(profile), 200L);
            return;
        }

        Queue<CachedLocation> queue = cacheMap.computeIfAbsent(
            profile.name.toLowerCase(),
            k -> new ConcurrentLinkedQueue<>()
        );

        if (queue.size() < profile.preparations.cacheLocations) {
            if (refillingMap.putIfAbsent(profile.name.toLowerCase(), true) == null) {
                refillNext(profile, queue);
            }
        }
    }

    private void refillNext(RtpProfile profile, Queue<CachedLocation> queue) {
        if (queue.size() >= profile.preparations.cacheLocations || !plugin.isEnabled()) {
            refillingMap.remove(profile.name.toLowerCase());
            return;
        }

        World landingWorld = plugin.getServer().getWorld(profile.landingWorld);
        if (landingWorld == null) {
            refillingMap.remove(profile.name.toLowerCase());
            return;
        }

        Location center = landingWorld.getSpawnLocation();
        safeLocationService.findSafeLocationAsync(profile, center).thenAccept(loc -> {
            queue.add(new CachedLocation(
                profile.name,
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
            ));
            if (plugin.getConfigHandler().getConfig().logging.rtpForQueue) {
                plugin.getComponentLogger().info("Pre-calculated safe location for profile " + profile.name + ": " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
            // Continue refilling recursively
            refillNext(profile, queue);
        }).exceptionally(ex -> {
            // Delay retry on failure to avoid log spam / high load
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> refillNext(profile, queue), 100L);
            return null;
        });
    }

    @Override
    public int getCacheSize(RtpProfile profile) {
        Queue<CachedLocation> queue = cacheMap.get(profile.name.toLowerCase());
        return queue != null ? queue.size() : 0;
    }

    @Override
    public void startRefillTask() {
        // Load cached locations from database
        List<CachedLocation> loaded = lunatech.jetrtp.database.Queries.LocationCache.load();
        for (var loc : loaded) {
            cacheMap.computeIfAbsent(loc.profileName().toLowerCase(), k -> new ConcurrentLinkedQueue<>()).add(loc);
        }

        // Trigger initial fill for all enabled profiles
        for (RtpProfile profile : plugin.getConfigHandler().getProfiles().values()) {
            refillCacheAsync(profile);
        }
    }

    @Override
    public void shutdown() {
        List<CachedLocation> toSave = new java.util.ArrayList<>();
        for (var entry : cacheMap.entrySet()) {
            toSave.addAll(entry.getValue());
        }
        lunatech.jetrtp.database.Queries.LocationCache.save(toSave);
        cacheMap.clear();
        refillingMap.clear();
    }

    @Override
    public long getHits(RtpProfile profile) {
        var adder = hitMap.get(profile.name.toLowerCase());
        return adder != null ? adder.sum() : 0L;
    }

    @Override
    public long getMisses(RtpProfile profile) {
        var adder = missMap.get(profile.name.toLowerCase());
        return adder != null ? adder.sum() : 0L;
    }

    @Override
    public double getAverageSearchTime(RtpProfile profile) {
        var sumTime = totalSearchTimeMap.get(profile.name.toLowerCase());
        var count = searchCountMap.get(profile.name.toLowerCase());
        if (sumTime == null || count == null || count.sum() == 0) {
            return 0.0;
        }
        return (double) sumTime.sum() / count.sum();
    }

    @Override
    public void recordHit(RtpProfile profile) {
        hitMap.computeIfAbsent(profile.name.toLowerCase(), k -> new java.util.concurrent.atomic.LongAdder()).increment();
    }

    @Override
    public void recordMiss(RtpProfile profile) {
        missMap.computeIfAbsent(profile.name.toLowerCase(), k -> new java.util.concurrent.atomic.LongAdder()).increment();
    }

    @Override
    public void recordSearchTime(RtpProfile profile, long durationMs) {
        totalSearchTimeMap.computeIfAbsent(profile.name.toLowerCase(), k -> new java.util.concurrent.atomic.LongAdder()).add(durationMs);
        searchCountMap.computeIfAbsent(profile.name.toLowerCase(), k -> new java.util.concurrent.atomic.LongAdder()).increment();
    }
}
