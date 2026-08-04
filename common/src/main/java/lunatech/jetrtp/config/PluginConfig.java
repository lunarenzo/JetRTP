package lunatech.jetrtp.config;

import lunatech.jetrtp.config.exception.ConfigValidationException;
import lunatech.jetrtp.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Map;

@ConfigSerializable
public class PluginConfig implements VersionedConfig {
    @Comment("Do not change this value!")
    public int configVersion = 2;

    @Override
    @Exclude
    public int configVersion() {
        return configVersion;
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

    @Comment("Update Checker Settings")
    public UpdateChecker updateChecker = new UpdateChecker();

    @ConfigSerializable
    public static class UpdateChecker {
        @Comment("Should the plugin check for plugin updates on startup?")
        public boolean enabled = true;

        @Comment("Send update notifications to the console?")
        public boolean console = true;

        @Comment("Send update notifications to opped players on join?")
        public boolean op = true;
    }

    @Comment("Language, specify the language file to use, for jetrtp `en_US` which will load `/lang/en_US.json`")
    public String language = "en_US";

    @Comment("RTP on first join settings")
    @Setting("rtp-on-first-join")
    public RtpOnFirstJoin rtpOnFirstJoin = new RtpOnFirstJoin();

    @ConfigSerializable
    public static class RtpOnFirstJoin {
        public boolean enabled = false;
        public String settings = "default-settings";
    }

    @Comment("RTP on death settings")
    @Setting("rtp-on-death")
    public RtpOnDeath rtpOnDeath = new RtpOnDeath();

    @ConfigSerializable
    public static class RtpOnDeath {
        public boolean enabled = false;
        public String settings = "default-settings";
        @Setting("respect-beds")
        public boolean respectBeds = true;
        @Setting("respect-anchors")
        public boolean respectAnchors = true;
        @Setting("require-permission")
        public boolean requirePermission = true;
    }

    @Comment("Location cache filler settings")
    @Setting("location-cache-filler")
    public LocationCacheFiller locationCacheFiller = new LocationCacheFiller();

    @ConfigSerializable
    public static class LocationCacheFiller {
        public boolean enabled = true;
        @Setting("recheck-time")
        public int recheckTime = 1800;
        @Setting("between-time")
        public double betweenTime = 0.0;
        @Setting("async-wait-timeout")
        public int asyncWaitTimeout = 10;
    }

    @Comment("Land claim support settings")
    @Setting("land-claim-support")
    public LandClaimSupport landClaimSupport = new LandClaimSupport();

    @ConfigSerializable
    public static class LandClaimSupport {
        @Setting("force-disable-all")
        public boolean forceDisableAll = false;
        @Setting("grief-prevention")
        public boolean griefPrevention = true;
        @Setting("world-guard")
        public boolean worldGuard = true;
        @Setting("husk-towns")
        public boolean huskTowns = true;
        @Setting("lands")
        public boolean lands = true;
    }

    @Comment("Logging settings")
    public LoggingSettings logging = new LoggingSettings();

    @ConfigSerializable
    public static class LoggingSettings {
        @Setting("rtp-on-player-join")
        public boolean rtpOnPlayerJoin = true;
        @Setting("rtp-on-respawn")
        public boolean rtpOnRespawn = true;
        @Setting("rtp-on-command")
        public boolean rtpOnCommand = true;
        @Setting("rtp-on-force-command")
        public boolean rtpOnForceCommand = true;
        @Setting("rtp-for-queue")
        public boolean rtpForQueue = false;
    }

    @Comment("Debug mode")
    public boolean debug = false;

    @Comment("RTP GUI Settings")
    public RtpGuiConfig gui = new RtpGuiConfig();

    @ConfigSerializable
    public static class RtpGuiConfig {
        @Comment("Should the GUI menu be shown when players run /rtp?")
        public boolean enabled = true;

        @Comment("The default profile to execute when GUI is disabled and /rtp is run without arguments")
        @Setting("default-profile")
        public String defaultProfile = "default-settings";

        @Comment("Title of the GUI menu (supports MiniMessage & color codes via ColorParser)")
        public String title = "<dark_gray>Random Teleport Destinations";

        @Comment("Number of rows for the GUI (1-6)")
        public int rows = 3;

        @Comment("The background fill item material")
        @Setting("fill-item")
        public String fillItem = "GRAY_STAINED_GLASS_PANE";

        @Comment("Custom items mapped to slots in the GUI")
        public Map<String, RtpGuiItemConfig> items = Map.of(
            "10", new RtpGuiItemConfig("COMPASS", "<green>Overworld RTP",
                java.util.List.of("<gray>Click to random teleport to this destination!", "", "<gray>Cooldown: <yellow>30s", "<gray>Cost: <yellow>$0.0"),
                "rtp:default-settings"),
            "12", new RtpGuiItemConfig("NETHERRACK", "<red>Nether RTP",
                java.util.List.of("<gray>Click to random teleport to the Nether!", "", "<gray>Cooldown: <yellow>30s", "<gray>Cost: <yellow>$0.0"),
                "rtp:nether-rtp"),
            "14", new RtpGuiItemConfig("ENDER_PEARL", "<purple>End RTP",
                java.util.List.of("<gray>Click to random teleport to the End!", "", "<gray>Cooldown: <yellow>30s", "<gray>Cost: <yellow>$0.0"),
                "rtp:end-rtp")
        );
    }

    @ConfigSerializable
    public static class RtpGuiItemConfig {
        public String material;
        public String name;
        public java.util.List<String> lore;
        public String action;

        public RtpGuiItemConfig() {}

        public RtpGuiItemConfig(String material, String name, java.util.List<String> lore, String action) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.action = action;
        }
    }
}
