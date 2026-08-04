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
import org.bukkit.World;
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

        Location potentialLoc = getPotentialLocation(profile, center);
        if (potentialLoc == null) {
            findNextAttempt(profile, center, future, attempts + 1);
            return;
        }

        World world = potentialLoc.getWorld();
        world.getChunkAtAsync(potentialLoc).thenAccept(chunk -> {
            if (chunk == null) {
                findNextAttempt(profile, center, future, attempts + 1);
                return;
            }

            boolean isNether = world.getEnvironment() == World.Environment.NETHER;
            boolean isMiddleOut = profile.locationCheckingProfile.equalsIgnoreCase("c")
                || (profile.locationCheckingProfile.equalsIgnoreCase("a") && isNether);

            if (isMiddleOut) {
                SafeLocationUtils.INSTANCE.dropToMiddle(potentialLoc, profile.bounds.low, profile.bounds.high, world);
            } else {
                SafeLocationUtils.INSTANCE.dropToGround(potentialLoc, profile.bounds.low, profile.bounds.high, world);
            }

            int x = potentialLoc.getBlockX();
            int z = potentialLoc.getBlockZ();
            int y = potentialLoc.getBlockY();

            if (y >= profile.bounds.low && y < profile.bounds.high) {
                Material standOn = world.getType(x, y, z);
                Material legs = world.getType(x, y + 1, z);
                Material head = world.getType(x, y + 2, z);

                if (SafeLocationUtils.INSTANCE.isSafeToBeOn(standOn)
                    && SafeLocationUtils.INSTANCE.isSafeToBeIn(legs)
                    && SafeLocationUtils.INSTANCE.isSafeToBeIn(head)) {

                    if (!claimService.isInsideClaim(potentialLoc)
                        && world.getWorldBorder().isInside(potentialLoc)) {

                        org.bukkit.block.Biome biome = world.getBiome(x, y, z);
                        boolean isExcluded = profile.resolvedExcludedBiomes != null && profile.resolvedExcludedBiomes.contains(biome);

                        if (!isExcluded) {
                            Location target = potentialLoc.clone().add(0.5, 1.0, 0.5);
                            target.setYaw(java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 360.0f);
                            future.complete(target);
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

    private Location getPotentialLocation(RtpProfile profile, Location center) {
        String distName = profile.distribution.toLowerCase();
        DistributionConfig dist = plugin.getConfigHandler().getDistributions().get(distName);
        if (dist == null) {
            dist = new DistributionConfig();
        }

        long coords;
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
            coords = RandomCords.getRandXyCircle(
                (int) maxRadius,
                (int) minRadius,
                dist.gaussianDistribution.enabled ? dist.gaussianDistribution.shrink : 0,
                dist.gaussianDistribution.center
            );
        } else if (dist.shape.equalsIgnoreCase("rectangle")) {
            if (dist.gap.enabled) {
                coords = RandomCords.getRandXyRectangle(
                    dist.size.xWidth / 2,
                    dist.size.zWidth / 2,
                    dist.gap.xWidth / 2,
                    dist.gap.zWidth / 2,
                    dist.gap.xCenter,
                    dist.gap.zCenter
                );
            } else {
                coords = RandomCords.getRandXyRectangle(dist.size.xWidth / 2, dist.size.zWidth / 2);
            }
        } else { // square
            if (dist.gaussianDistribution.enabled) {
                coords = RandomCords.getRandXySquare(
                    (int) maxRadius,
                    (int) minRadius,
                    dist.gaussianDistribution.shrink,
                    dist.gaussianDistribution.center
                );
            } else {
                coords = RandomCords.getRandXySquare((int) maxRadius, (int) minRadius);
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

        int x = RandomCords.unpackX(coords);
        int z = RandomCords.unpackZ(coords);
        return new Location(center.getWorld(), x + centerX, 255, z + centerZ);
    }
}
