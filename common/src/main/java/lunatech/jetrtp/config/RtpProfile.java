package lunatech.jetrtp.config;

import lunatech.jetrtp.config.exception.ConfigValidationException;
import lunatech.jetrtp.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Biome;

@ConfigSerializable
public class RtpProfile implements VersionedConfig {

    public transient String name;

    public boolean enabled = true;

    @Setting("command-enabled")
    public boolean commandEnabled = true;

    @Setting("require-explicit-permission")
    public boolean requireExplicitPermission = false;

    public int priority = 1;

    @Setting("landing-world")
    public String landingWorld = "world";

    @Setting("call-from-worlds")
    public List<String> callFromWorlds = new ArrayList<>(List.of("world.*"));

    public String distribution = "default-symmetric";

    @Setting("location-checking-profile")
    public String locationCheckingProfile = "a";

    public int cooldown = 30;

    public Warmup warmup = new Warmup();

    @ConfigSerializable
    public static class Warmup {
        public int time = 5;
        @Setting("cancel-on-move")
        public boolean cancelOnMove = true;
        @Setting("count-down")
        public boolean countDown = true;
        @Setting("title-countdown")
        public String titleCountdown = "&a&lʀᴀɴᴅᴏᴍ ᴛᴘ";
        @Setting("subtitle-countdown")
        public String subtitleCountdown = "&fᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ɪɴ &a%time%&f...";
        @Setting("sound-countdown")
        public String soundCountdown = "BLOCK_NOTE_BLOCK_PLING";
        @Setting("sound-countdown-pitch-increase")
        public boolean soundCountdownPitchIncrease = true;
        @Setting("title-success")
        public String titleSuccess = "&a&lᴀʀʀɪᴠᴇᴅ!";
        @Setting("subtitle-success")
        public String subtitleSuccess = "&fʏᴏᴜ ʜᴀᴠᴇ ʙᴇᴇɴ ʀᴀɴᴅᴏᴍʟʏ ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ!";
        @Setting("sounds-success")
        public List<String> soundsSuccess = new ArrayList<>(List.of(
            "ENTITY_ENDERMAN_TELEPORT:1.0:1.0",
            "ENTITY_WITHER_SPAWN:0.4:1.5"
        ));
        @Setting("title-cancel")
        public String titleCancel = "&c&lᴄᴀɴᴄᴇʟʟᴇᴅ";
        @Setting("subtitle-cancel")
        public String subtitleCancel = "&fᴛᴇʟᴇᴘᴏʀᴛᴀᴛɪᴏɴ ʜᴀѕ ʙᴇᴇɴ ɪɴᴛᴇʀʀᴜᴘᴛᴇᴅ!";
        @Setting("sounds-cancel")
        public List<String> soundsCancel = new ArrayList<>(List.of(
            "BLOCK_FIRE_EXTINGUISH:1.0:1.0"
        ));
    }

    @Setting("then-execute")
    public List<String> thenExecute = new ArrayList<>(List.of(
        "tellraw %PLAYER% {\"text\":\"You have been teleported to %LOCATION% in %WORLD%!\"}"
    ));

    public double cost = 0.0;

    public Bounds bounds = new Bounds();

    @ConfigSerializable
    public static class Bounds {
        public int low = 32;
        public int high = 255;
    }

    @Setting("check-radius")
    public CheckRadius checkRadius = new CheckRadius();

    @ConfigSerializable
    public static class CheckRadius {
        @Setting("x-z")
        public int xZ = 2;
        public int vert = 2;
    }

    @Setting("max-attempts")
    public MaxAttempts maxAttempts = new MaxAttempts();

    @ConfigSerializable
    public static class MaxAttempts {
        public int value = 10;
    }

    public Preparations preparations = new Preparations();

    @ConfigSerializable
    public static class Preparations {
        @Setting("cache-locations")
        public int cacheLocations = 10;
    }

    @Setting("excluded-biomes")
    public List<String> excludedBiomes = new ArrayList<>();

    @Setting("prefer-sync-tp")
    public PreferSyncTp preferSyncTp = new PreferSyncTp();

    @ConfigSerializable
    public static class PreferSyncTp {
        public boolean command = false;
    }

    public transient EnumSet<Biome> resolvedExcludedBiomes;

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
        resolvedExcludedBiomes = EnumSet.noneOf(Biome.class);
        if (excludedBiomes != null) {
            for (String ex : excludedBiomes) {
                String term = ex.toLowerCase();
                for (Biome biome : Biome.values()) {
                    if (biome.name().toLowerCase().contains(term)) {
                        resolvedExcludedBiomes.add(biome);
                    }
                }
            }
        }
    }
}
