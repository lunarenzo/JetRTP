package lunatech.jetrtp.utility;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * Utility for evaluating block safety and scanning coordinates.
 */
public final class SafeLocationUtils {

    public static final SafeLocationUtils INSTANCE = new SafeLocationUtils();

    private SafeLocationUtils() {}

    /**
     * Checks if the material is safe for a player to be inside (foot/head space).
     */
    public boolean isSafeToBeIn(Material mat) {
        if (mat == null) return false;
        return switch (mat) {
            case AIR, CAVE_AIR, VOID_AIR, SNOW, FERN, LARGE_FERN, VINE, SHORT_GRASS, TALL_GRASS, GLOW_LICHEN, MOSS_CARPET, GLOW_BERRIES -> true;
            default -> false;
        };
    }

    /**
     * Checks if the material is safe to stand on.
     */
    public boolean isSafeToBeOn(Material mat) {
        if (mat == null) return false;
        return switch (mat) {
            case LAVA, MAGMA_BLOCK, WATER, AIR, CAVE_AIR, VOID_AIR, CACTUS, SEAGRASS, KELP, TALL_SEAGRASS, LILY_PAD, BAMBOO, BAMBOO_SAPLING, SMALL_DRIPLEAF, BIG_DRIPLEAF, BIG_DRIPLEAF_STEM, POINTED_DRIPSTONE -> false;
            default -> true;
        };
    }

    /**
     * Checks if the material is tree leaves.
     */
    public boolean isTreeLeaves(Material mat) {
        if (mat == null) return false;
        return switch (mat) {
            case ACACIA_LEAVES, BIRCH_LEAVES, DARK_OAK_LEAVES, JUNGLE_LEAVES, OAK_LEAVES, SPRUCE_LEAVES, AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES, CHERRY_LEAVES, MANGROVE_LEAVES -> true;
            default -> false;
        };
    }

    public boolean isSafeToGoThrough(Material mat) {
        return isTreeLeaves(mat);
    }

    /**
     * Drops location to the ground using direct Chunk checks.
     */
    public void dropToGround(Location loc, int lowBound, int highBound, Chunk chunk) {
        if (loc.getY() > highBound) loc.setY(highBound);
        int localX = loc.getBlockX() & 15;
        int localZ = loc.getBlockZ() & 15;

        // If start in solid block, wait until outside
        while (loc.getBlockY() > lowBound) {
            Material mat = chunk.getBlock(localX, loc.getBlockY(), localZ).getType();
            if (isSafeToBeIn(mat) || isSafeToGoThrough(mat)) break;
            loc.add(0, -1, 0);
        }

        // Search for solid ground
        while (loc.getBlockY() > lowBound) {
            Material mat = chunk.getBlock(localX, loc.getBlockY(), localZ).getType();
            if (!isSafeToBeIn(mat) && !isSafeToGoThrough(mat)) break;
            loc.add(0, -1, 0);
        }
    }

    /**
     * Drops location to the middle of the vertical bounds (Nether check).
     */
    public void dropToMiddle(Location loc, int lowBound, int highBound, Chunk chunk) {
        loc.setY((highBound + lowBound) / 2.0);
        int localX = loc.getBlockX() & 15;
        int localZ = loc.getBlockZ() & 15;

        int change = 0;
        int direction = 1;
        boolean upWasSolid = false;
        boolean downWasAir = false;

        while (loc.getY() > lowBound && loc.getY() < highBound) {
            Material mat = chunk.getBlock(localX, loc.getBlockY(), localZ).getType();

            if (direction == -1) {
                if (upWasSolid && isSafeToBeIn(mat)) break;
            } else {
                if (downWasAir && isSafeToBeOn(mat)) break;
            }

            if (direction == 1) {
                upWasSolid = isSafeToBeOn(mat);
            } else {
                downWasAir = isSafeToBeIn(mat) || isSafeToGoThrough(mat);
            }

            loc.add(0, change * direction, 0);
            if (direction == -1) {
                change++;
            }
            direction *= -1;
            loc.add(0, -change * direction, 0);
        }
    }
}
