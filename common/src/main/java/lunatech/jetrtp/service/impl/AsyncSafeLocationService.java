package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.DistributionConfig;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.service.LandClaimService;
import lunatech.jetrtp.service.SafeLocationService;
import lunatech.jetrtp.utility.RandomCords;
import lunatech.jetrtp.utility.SafeLocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import java.util.concurrent.CompletableFuture;

public class AsyncSafeLocationService implements SafeLocationService {

    private final AbstractJetRTP plugin;
    private final LandClaimService claimService;

    public AsyncSafeLocationService(AbstractJetRTP plugin, LandClaimService claimService) {
        this.plugin = plugin;
        this.claimService = claimService;
    }

    @Override
    public CompletableFuture<Location> findSafeLocationAsync(RtpProfile profile, Location center) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        findNextAttempt(profile, center, future, 0);
        return future;
    }

    private void findNextAttempt(RtpProfile profile, Location center, CompletableFuture<Location> future, int attempts) {
        if (attempts >= profile.maxAttempts.value) {
            future.completeExceptionally(new RuntimeException("Could not find a safe location after " + profile.maxAttempts.value + " attempts."));
            return;
        }

        long packed = getPotentialCoords(profile, center);
        int targetX = RandomCords.getX(packed);
        int targetZ = RandomCords.getZ(packed);

        int chunkX = targetX >> 4;
        int chunkZ = targetZ >> 4;

        center.getWorld().getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            if (chunk == null) {
                findNextAttempt(profile, center, future, attempts + 1);
                return;
            }

            boolean isNether = center.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER;
            boolean isMiddleOut = profile.locationCheckingProfile.equalsIgnoreCase("c")
                || (profile.locationCheckingProfile.equalsIgnoreCase("a") && isNether);

            int localX = targetX & 15;
            int localZ = targetZ & 15;
            int y;

            if (isMiddleOut) {
                y = SafeLocationUtils.INSTANCE.dropToMiddle(localX, 255, localZ, profile.bounds.low, profile.bounds.high, chunk);
            } else {
                y = SafeLocationUtils.INSTANCE.dropToGround(localX, 255, localZ, profile.bounds.low, profile.bounds.high, chunk);
            }

            if (y >= profile.bounds.low && y < profile.bounds.high) {
                Material standOn = chunk.getType(localX, y, localZ);
                Material legs = chunk.getType(localX, y + 1, localZ);
                Material head = chunk.getType(localX, y + 2, localZ);

                if (SafeLocationUtils.INSTANCE.isSafeToBeOn(standOn)
                    && SafeLocationUtils.INSTANCE.isSafeToBeIn(legs)
                    && SafeLocationUtils.INSTANCE.isSafeToBeIn(head)) {

                    Location targetLocation = new Location(center.getWorld(), targetX + 0.5, y + 1.0, targetZ + 0.5);

                    if (!claimService.isInsideClaim(targetLocation)
                        && center.getWorld().getWorldBorder().isInside(targetLocation)) {

                        String biomeName = targetLocation.getWorld().getBiome(targetLocation).name();
                        boolean isExcluded = false;
                        for (String excluded : profile.excludedBiomes) {
                            if (biomeName.equalsIgnoreCase(excluded) || biomeName.toLowerCase().contains(excluded.toLowerCase())) {
                                isExcluded = true;
                                break;
                            }
                        }

                        if (!isExcluded) {
                            targetLocation.setYaw(java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 360.0f);
                            future.complete(targetLocation);
                            return;
                        }
                    }
                }
            }

            // Retry next attempt
            findNextAttempt(profile, center, future, attempts + 1);
        }).exceptionally(ex -> {
            findNextAttempt(profile, center, future, attempts + 1);
            return null;
        });
    }

    private long getPotentialCoords(RtpProfile profile, Location center) {
        String distName = profile.distribution.toLowerCase();
        DistributionConfig dist = plugin.getConfigHandler().getDistributions().get(distName);
        if (dist == null) {
            dist = new DistributionConfig();
        }

        long coordsPacked;
        double maxRadius = dist.radius.max;
        double minRadius = dist.radius.min;
        double centerX = 0;
        double centerZ = 0;
        boolean isWorldBorder = distName.equalsIgnoreCase("world-border") || dist.shape.equalsIgnoreCase("world-border");

        if (isWorldBorder) {
            org.bukkit.WorldBorder wb = center.getWorld().getWorldBorder();
            maxRadius = Math.max(0, (wb.getSize() / 2.0) - 10.0);
            centerX = wb.getCenter().getX();
            centerZ = wb.getCenter().getZ();
        }

        if (isWorldBorder || dist.shape.equalsIgnoreCase("circle")) {
            coordsPacked = RandomCords.getRandXyCircle(
                (int) maxRadius,
                (int) minRadius,
                dist.gaussianDistribution.enabled ? dist.gaussianDistribution.shrink : 0,
                dist.gaussianDistribution.center
            );
        } else if (dist.shape.equalsIgnoreCase("rectangle")) {
            if (dist.gap.enabled) {
                coordsPacked = RandomCords.getRandXyRectangle(
                    dist.size.xWidth / 2,
                    dist.size.zWidth / 2,
                    dist.gap.xWidth / 2,
                    dist.gap.zWidth / 2,
                    dist.gap.xCenter,
                    dist.gap.zCenter
                );
            } else {
                coordsPacked = RandomCords.getRandXyRectangle(dist.size.xWidth / 2, dist.size.zWidth / 2);
            }
        } else { // square
            if (dist.gaussianDistribution.enabled) {
                coordsPacked = RandomCords.getRandXySquare(
                    (int) maxRadius,
                    (int) minRadius,
                    dist.gaussianDistribution.shrink,
                    dist.gaussianDistribution.center
                );
            } else {
                coordsPacked = RandomCords.getRandXySquare((int) maxRadius, (int) minRadius);
            }
        }

        if (!isWorldBorder) {
            if (dist.center.option.equalsIgnoreCase("a")) { // world spawn
                Location spawn = center.getWorld().getSpawnLocation();
                centerX = spawn.getX();
                centerZ = spawn.getZ();
            } else if (dist.center.option.equalsIgnoreCase("b")) { // player location
                centerX = center.getX();
                centerZ = center.getZ();
            } else { // custom
                centerX = dist.center.cCustom.x;
                centerZ = dist.center.cCustom.z;
            }
        }

        int finalX = (int) (RandomCords.getX(coordsPacked) + centerX);
        int finalZ = (int) (RandomCords.getZ(coordsPacked) + centerZ);
        return RandomCords.pack(finalX, finalZ);
    }
}
