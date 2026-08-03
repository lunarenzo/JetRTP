package lunatech.jetrtp.config;

import lunatech.jetrtp.config.exception.ConfigValidationException;
import lunatech.jetrtp.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Map;

@ConfigSerializable
public class DistributionConfig implements VersionedConfig {
    
    public String shape = "square";
    
    public Center center = new Center();

    @ConfigSerializable
    public static class Center {
        public String option = "a";
        @Setting("c-custom")
        public CustomCenter cCustom = new CustomCenter();
    }

    @ConfigSerializable
    public static class CustomCenter {
        public int x = 0;
        public int z = 0;
    }

    public Radius radius = new Radius();

    @ConfigSerializable
    public static class Radius {
        public int max = 2000;
        public int min = 1000;
    }

    @Setting("gaussian-distribution")
    public GaussianDistribution gaussianDistribution = new GaussianDistribution();

    @ConfigSerializable
    public static class GaussianDistribution {
        public boolean enabled = false;
        public double shrink = 4.0;
        public double center = 0.25;
    }

    public Size size = new Size();

    @ConfigSerializable
    public static class Size {
        @Setting("x-width")
        public int xWidth = 2000;
        @Setting("z-width")
        public int zWidth = 1600;
    }

    public Gap gap = new Gap();

    @ConfigSerializable
    public static class Gap {
        public boolean enabled = false;
        @Setting("x-width")
        public int xWidth = 700;
        @Setting("z-width")
        public int zWidth = 700;
        @Setting("x-center")
        public int xCenter = 150;
        @Setting("z-center")
        public int zCenter = 150;
    }

    @Override
    @Exclude
    public int configVersion() {
        return 1;
    }

    @Override
    @Exclude
    public @NotNull Map<Integer, Migration> migrations() {
        return Map.of();
    }

    @Override
    @Exclude
    public void validate() throws ConfigValidationException {
    }
}
