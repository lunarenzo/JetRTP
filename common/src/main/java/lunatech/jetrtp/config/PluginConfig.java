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

    @Comment("RTP GUI Inventory Settings")
    public GuiSettings gui = new GuiSettings();

    @ConfigSerializable
    public static class GuiSettings {
        @Comment("Should the GUI be opened when running /rtp command without arguments? If false, they will be teleported using default profile settings.")
        public boolean enabled = true;

        @Comment("The default profile used when running /rtp without arguments and GUI is disabled.")
        @Setting("default-profile")
        public String defaultProfile = "default-settings";

        @Comment("Title of the profile selection menu GUI.")
        public String title = "<dark_gray>Random Teleport Destinations";

        @Comment("Size of the GUI inventory (must be a multiple of 9, e.g. 9, 18, 27, 36, 45, 54).")
        public int size = 27;

        @Comment("The default filler material for slots that are not occupied by profiles.")
        @Setting("filler-material")
        public String fillerMaterial = "GRAY_STAINED_GLASS_PANE";

        @Comment("Map of slot index to profile name. Defines the custom GUI layout.")
        public Map<Integer, String> layout = Map.of(
            10, "default-settings"
        );
    }
}
