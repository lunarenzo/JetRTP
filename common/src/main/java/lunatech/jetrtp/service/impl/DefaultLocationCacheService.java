package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.service.LocationCacheService;
import lunatech.jetrtp.service.SafeLocationService;
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
    private final Map<String, Queue<Location>> cacheMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> refillingMap = new ConcurrentHashMap<>();

    public DefaultLocationCacheService(AbstractJetRTP plugin, SafeLocationService safeLocationService) {
        this.plugin = plugin;
        this.safeLocationService = safeLocationService;
    }

    @Override
    public Location popCachedLocation(RtpProfile profile) {
        Queue<Location> queue = cacheMap.get(profile.name.toLowerCase());
        if (queue == null) {
            return null;
        }
        Location loc = queue.poll();
        if (loc != null) {
            // Trigger refill in the background
            refillCacheAsync(profile);
        }
        return loc;
    }

    @Override
    public void refillCacheAsync(RtpProfile profile) {
        if (!profile.enabled || profile.preparations.cacheLocations <= 0) {
            return;
        }

        Queue<Location> queue = cacheMap.computeIfAbsent(
            profile.name.toLowerCase(),
            k -> new ConcurrentLinkedQueue<>()
        );

        if (queue.size() < profile.preparations.cacheLocations) {
            if (refillingMap.putIfAbsent(profile.name.toLowerCase(), true) == null) {
                refillNext(profile, queue);
            }
        }
    }

    private void refillNext(RtpProfile profile, Queue<Location> queue) {
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
            queue.add(loc);
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
        Queue<Location> queue = cacheMap.get(profile.name.toLowerCase());
        return queue != null ? queue.size() : 0;
    }

    @Override
    public void startRefillTask() {
        // Load cached locations from database
        List<lunatech.jetrtp.model.CachedLocation> loaded = lunatech.jetrtp.database.Queries.LocationCache.load();
        for (var loc : loaded) {
            World world = plugin.getServer().getWorld(loc.worldName());
            if (world != null) {
                Location location = new Location(world, loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
                cacheMap.computeIfAbsent(loc.profileName().toLowerCase(), k -> new ConcurrentLinkedQueue<>()).add(location);
            }
        }

        // Trigger initial fill for all enabled profiles
        for (RtpProfile profile : plugin.getConfigHandler().getProfiles().values()) {
            refillCacheAsync(profile);
        }
    }

    @Override
    public void shutdown() {
        List<lunatech.jetrtp.model.CachedLocation> toSave = new java.util.ArrayList<>();
        for (var entry : cacheMap.entrySet()) {
            String profileName = entry.getKey();
            for (Location loc : entry.getValue()) {
                toSave.add(new lunatech.jetrtp.model.CachedLocation(
                    profileName,
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getPitch()
                ));
            }
        }
        lunatech.jetrtp.database.Queries.LocationCache.save(toSave);
        cacheMap.clear();
        refillingMap.clear();
    }
}
