package lunatech.jetrtp.utility;

import org.bukkit.Chunk;
import org.bukkit.Material;

/**
 * Utility for evaluating block safety and scanning coordinates.
 * Completely optimized to run on primitive values with zero allocations.
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
     * Drops Y coordinate to the ground using direct Chunk checks.
     */
    public int dropToGround(int localX, int startY, int localZ, int lowBound, int highBound, Chunk chunk) {
        int y = Math.min(startY, highBound);

        // If start in solid block, wait until outside
        while (y > lowBound) {
            Material mat = chunk.getBlockType(localX, y, localZ);
            if (isSafeToBeIn(mat) || isSafeToGoThrough(mat)) break;
            y--;
        }

        // Search for solid ground
        while (y > lowBound) {
            Material mat = chunk.getBlockType(localX, y, localZ);
            if (!isSafeToBeIn(mat) && !isSafeToGoThrough(mat)) break;
            y--;
        }
        return y;
    }

    /**
     * Drops Y coordinate to the middle of the vertical bounds (Nether check).
     */
    public int dropToMiddle(int localX, int startY, int localZ, int lowBound, int highBound, Chunk chunk) {
        int y = (highBound + lowBound) / 2;
        int change = 0;
        int direction = 1;
        boolean upWasSolid = false;
        boolean downWasAir = false;

        while (y > lowBound && y < highBound) {
            Material mat = chunk.getBlockType(localX, y, localZ);

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

            y += change * direction;
            if (direction == -1) {
                change++;
            }
            direction *= -1;
            y += -change * direction;
        }
        return y;
    }
}
