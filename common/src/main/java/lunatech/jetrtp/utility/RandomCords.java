package lunatech.jetrtp.utility;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating random coordinates in various shapes.
 * Optimized using ThreadLocalRandom and primitive math functions.
 */
public final class RandomCords {

    private RandomCords() {}

    /**
     * Generates random coordinates within a circle (donut shape with min/max radius).
     */
    public static double[] getRandXyCircle(int radiusMax, int radiusMin, double gaussianShrink, double gaussianCenter) {
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

        return new double[]{
            radius * Math.cos(angle),
            radius * Math.sin(angle)
        };
    }

    /**
     * Generates random coordinates within a square (with exclusion zone).
     */
    public static double[] getRandXySquare(int radiusMax, int radiusMin) {
        return getRandXySquare(radiusMax, radiusMin, 0, 0);
    }

    /**
     * Generates random coordinates within a square with optional gaussian distribution.
     */
    public static double[] getRandXySquare(int radiusMax, int radiusMin, double gaussianShrink, double gaussianCenter) {
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

        return new double[]{x, z};
    }

    /**
     * Generates random coordinates within a rectangle.
     */
    public static double[] getRandXyRectangle(int xRadius, int zRadius) {
        var rand = ThreadLocalRandom.current();
        double x = (rand.nextDouble() * 2 - 1) * xRadius;
        double z = (rand.nextDouble() * 2 - 1) * zRadius;
        return new double[]{x, z};
    }

    /**
     * Generates random coordinates within a rectangle with a gap (exclusion zone).
     */
    public static double[] getRandXyRectangle(int xRadius, int zRadius, int gapXRadius, int gapZRadius, int gapXCenter, int gapZCenter) {
        var rand = ThreadLocalRandom.current();
        double x, z;

        do {
            x = (rand.nextDouble() * 2 - 1) * xRadius;
            z = (rand.nextDouble() * 2 - 1) * zRadius;
        } while (Math.abs(x - gapXCenter) < gapXRadius && Math.abs(z - gapZCenter) < gapZRadius);

        return new double[]{x, z};
    }

    /**
     * Converts a double array to an int array (truncating decimals).
     */
    public static int[] asIntArray2w(double[] arr) {
        return new int[]{(int) arr[0], (int) arr[1]};
    }
}
