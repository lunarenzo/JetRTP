package lunatech.jetrtp.utility;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating random coordinates in various shapes.
 * Packed as primitive long to eliminate heap allocation.
 */
public final class RandomCords {

    private RandomCords() {}

    public static int getX(long packed) {
        return (int) (packed >> 32);
    }

    public static int getZ(long packed) {
        return (int) packed;
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Generates random coordinates within a circle (donut shape with min/max radius).
     */
    public static long getRandXyCircle(int radiusMax, int radiusMin, double gaussianShrink, double gaussianCenter) {
        var rand = ThreadLocalRandom.current();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double radius;

        if (gaussianShrink > 0) {
            // Gaussian distribution
            double gaussian = rand.nextGaussian() * gaussianShrink + gaussianCenter;
            gaussian = Math.max(0, Math.min(1, (gaussian + 3) / 6)); // Normalize to 0-1
            radius = radiusMin + gaussian * (radiusMax - radiusMin);
        } else {
            // Uniform distribution within the annulus
            double minSq = (double) radiusMin * radiusMin;
            double maxSq = (double) radiusMax * radiusMax;
            radius = Math.sqrt(minSq + rand.nextDouble() * (maxSq - minSq));
        }

        int x = (int) (radius * Math.cos(angle));
        int z = (int) (radius * Math.sin(angle));
        return pack(x, z);
    }

    /**
     * Generates random coordinates within a square (with exclusion zone).
     */
    public static long getRandXySquare(int radiusMax, int radiusMin) {
        return getRandXySquare(radiusMax, radiusMin, 0, 0);
    }

    /**
     * Generates random coordinates within a square with optional gaussian distribution.
     */
    public static long getRandXySquare(int radiusMax, int radiusMin, double gaussianShrink, double gaussianCenter) {
        var rand = ThreadLocalRandom.current();
        double x, z;

        do {
            if (gaussianShrink > 0) {
                // Gaussian distribution
                double gx = rand.nextGaussian() * gaussianShrink + gaussianCenter;
                double gz = rand.nextGaussian() * gaussianShrink + gaussianCenter;
                gx = Math.max(-1, Math.min(1, gx / 3));
                gz = Math.max(-1, Math.min(1, gz / 3));
                x = gx * radiusMax;
                z = gz * radiusMax;
            } else {
                // Uniform distribution
                x = (rand.nextDouble() * 2 - 1) * radiusMax;
                z = (rand.nextDouble() * 2 - 1) * radiusMax;
            }
        } while (Math.abs(x) < radiusMin && Math.abs(z) < radiusMin);

        return pack((int) x, (int) z);
    }

    /**
     * Generates random coordinates within a rectangle.
     */
    public static long getRandXyRectangle(int xRadius, int zRadius) {
        var rand = ThreadLocalRandom.current();
        double x = (rand.nextDouble() * 2 - 1) * xRadius;
        double z = (rand.nextDouble() * 2 - 1) * zRadius;
        return pack((int) x, (int) z);
    }

    /**
     * Generates random coordinates within a rectangle with a gap (exclusion zone).
     */
    public static long getRandXyRectangle(int xRadius, int zRadius, int gapXRadius, int gapZRadius, int gapXCenter, int gapZCenter) {
        var rand = ThreadLocalRandom.current();
        double x, z;

        do {
            x = (rand.nextDouble() * 2 - 1) * xRadius;
            z = (rand.nextDouble() * 2 - 1) * zRadius;
        } while (Math.abs(x - gapXCenter) < gapXRadius && Math.abs(z - gapZCenter) < gapZRadius);

        return pack((int) x, (int) z);
    }
}
