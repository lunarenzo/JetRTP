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

    @Comment("Update Checker Settings\nControls whether JetRTP pings the update source on startup to check for new releases.")
    public UpdateChecker updateChecker = new UpdateChecker();

    @ConfigSerializable
    public static class UpdateChecker {
        @Comment("Enable or disable the update checker entirely.\nSet to false to skip the check on every startup (useful for air-gapped servers).")
        public boolean enabled = true;

        @Comment("Print update notifications to the server console.")
        public boolean console = true;

        @Comment("Send update notifications to operators (OP players) when they join the server.")
        public boolean op = true;
    }

    @Comment("Language file to load for all plugin messages.\nThe value maps to a file inside the plugin's lang/ folder.\nExample: 'en_US' loads lang/en_US.yml.\nTo add a custom translation, create a new file (e.g. lang/de_DE.yml) and set this to 'de_DE'.")
    public String language = "en_US";

    @Comment("RTP on First Join\nAutomatically teleports a player to a random location the FIRST time they ever join the server.\nDoes nothing on subsequent logins (checked via player.hasPlayedBefore()).\nPlayers with the permission 'jetrtp.nofirstjoinrtp' are excluded.")
    @Setting("rtp-on-first-join")
    public RtpOnFirstJoin rtpOnFirstJoin = new RtpOnFirstJoin();

    @ConfigSerializable
    public static class RtpOnFirstJoin {
        @Comment("Enable or disable the first-join random teleport feature.")
        public boolean enabled = false;
        @Comment("The name of the RTP profile (file in rtpSettings/) to use for the first-join teleport.")
        public String settings = "default-settings";
    }

    @Comment("RTP on Death (Respawn RTP)\nRandomly teleports a player to a new location when they respawn after dying.\nPre-cached locations are used for instant teleportation when available.\nIf no cache is ready, a location is found asynchronously and the player is teleported shortly after spawn.")
    @Setting("rtp-on-death")
    public RtpOnDeath rtpOnDeath = new RtpOnDeath();

    @ConfigSerializable
    public static class RtpOnDeath {
        @Comment("Enable or disable respawn random teleportation.")
        public boolean enabled = false;
        @Comment("The name of the RTP profile (file in rtpSettings/) to use for respawn teleportation.")
        public String settings = "default-settings";
        @Comment("If true, players who have a valid bed or respawn anchor set will NOT be randomly teleported on death.\nThey will respawn at their bed/anchor instead.")
        @Setting("respect-beds")
        public boolean respectBeds = true;
        @Comment("If true, players who have set a respawn anchor (in the Nether) will NOT be randomly teleported.")
        @Setting("respect-anchors")
        public boolean respectAnchors = true;
        @Comment("If true, only players with the permission 'jetrtp.rtpondeath' will be randomly teleported on death.")
        @Setting("require-permission")
        public boolean requirePermission = true;
    }

    @Comment("Location Cache Filler\nPre-generates and stores verified safe landing coordinates in the database.\nThis allows instant teleportation without any async lookup delay when a player runs /rtp.\nThe filler runs as a background async task to avoid impacting server TPS.")
    @Setting("location-cache-filler")
    public LocationCacheFiller locationCacheFiller = new LocationCacheFiller();

    @ConfigSerializable
    public static class LocationCacheFiller {
        @Comment("Enable or disable background cache pre-filling.\nDisabling this means all RTP calls must find a location on-demand, which may cause a brief delay.")
        public boolean enabled = true;
        @Comment("How often (in seconds) the filler checks and refills caches that are below their target size.\nDefault: 1800 (every 30 minutes).")
        @Setting("recheck-time")
        public int recheckTime = 1800;
        @Comment("Minimum time (in seconds) to wait between generating individual cache entries.\nUseful on low-end hardware to throttle chunk generation overhead.\nSet to 0.0 for maximum fill speed.")
        @Setting("between-time")
        public double betweenTime = 0.0;
        @Comment("Maximum time (in seconds) to wait for an async location search before giving up.\nIf the server is under heavy load and chunk loading stalls, this prevents the filler from hanging indefinitely.")
        @Setting("async-wait-timeout")
        public int asyncWaitTimeout = 10;
    }

    @Comment("Land Claim Support\nPrevents JetRTP from teleporting players into protected or claimed regions.\nEach integration is checked only if the corresponding plugin is installed on the server.")
    @Setting("land-claim-support")
    public LandClaimSupport landClaimSupport = new LandClaimSupport();

    @ConfigSerializable
    public static class LandClaimSupport {
        @Comment("Set to true to disable ALL land claim integrations globally, regardless of the individual settings below.")
        @Setting("force-disable-all")
        public boolean forceDisableAll = false;
        @Comment("Respect GriefPrevention claims. Players will not teleport inside claimed land.")
        @Setting("grief-prevention")
        public boolean griefPrevention = true;
        @Comment("Respect WorldGuard regions. Players will not teleport inside protected regions.")
        @Setting("world-guard")
        public boolean worldGuard = true;
        @Comment("Respect HuskTowns town claims. Players will not teleport inside town territories.")
        @Setting("husk-towns")
        public boolean huskTowns = true;
        @Comment("Respect Lands claims. Players will not teleport inside claimed land plots.")
        @Setting("lands")
        public boolean lands = true;
    }

    @Comment("Logging Settings\nControls which RTP events are printed to the server console.\nDisabling noisy entries reduces log file size on high-traffic servers.")
    public LoggingSettings logging = new LoggingSettings();

    @ConfigSerializable
    public static class LoggingSettings {
        @Comment("Log a message when a player is randomly teleported on first join.")
        @Setting("rtp-on-player-join")
        public boolean rtpOnPlayerJoin = true;
        @Comment("Log a message when a player is randomly teleported on respawn (death RTP).")
        @Setting("rtp-on-respawn")
        public boolean rtpOnRespawn = true;
        @Comment("Log a message when a player triggers an RTP via the /rtp command.")
        @Setting("rtp-on-command")
        public boolean rtpOnCommand = true;
        @Comment("Log a message when an admin forces an RTP on a player via /rtp force.")
        @Setting("rtp-on-force-command")
        public boolean rtpOnForceCommand = true;
        @Comment("Log a message when the cache filler generates a new queued location.\nDisabled by default to prevent log spam on servers with many profiles.")
        @Setting("rtp-for-queue")
        public boolean rtpForQueue = false;
    }

    @Comment("Debug Mode\nEnables verbose internal logging for diagnosing configuration issues, async task failures, or unexpected teleport behaviour.\nLeave disabled in production — it generates a large volume of log output.")
    public boolean debug = false;

    @Comment("Cooldown Storage Type\nDetermines where RTP cooldown timers are persisted between sessions.\n  PDC      - Stores cooldowns in the player's Persistent Data Container (local, no database required).\n             Best for single-server setups. Cooldowns reset if the player's data is wiped.\n  DATABASE - Stores cooldowns in the configured SQL database (see database.yml).\n             Required for multi-server networks (BungeeCord/Velocity) to share cooldowns across nodes.")
    @Setting("cooldown-storage-type")
    public String cooldownStorageType = "PDC";

    @Comment("RTP GUI Settings\nConfigures the interactive inventory menu shown when players run /rtp.")
    public RtpGuiConfig gui = new RtpGuiConfig();

    @ConfigSerializable
    public static class RtpGuiConfig {
        @Comment("Show the GUI inventory menu when players run /rtp.\nIf false, /rtp executes the 'default-profile' directly without opening a menu.")
        public boolean enabled = true;

        @Comment("The RTP profile used when GUI is disabled and /rtp is run without arguments.\nMust match the filename (without .yml) of a file in the rtpSettings/ directory.")
        @Setting("default-profile")
        public String defaultProfile = "default-settings";

        @Comment("Title displayed at the top of the GUI inventory.\nSupports MiniMessage tags (e.g. <red>, <bold>) and legacy color codes via ColorParser.")
        public String title = "<dark_gray>Random Teleport Destinations";

        @Comment("Number of rows in the GUI inventory. Valid range: 1-6 (each row = 9 slots).")
        public int rows = 3;

        @Comment("Material used to fill empty slots in the GUI as a decorative background.\nMust be a valid Bukkit Material name (e.g. GRAY_STAINED_GLASS_PANE, BLACK_STAINED_GLASS_PANE).")
        @Setting("fill-item")
        public String fillItem = "GRAY_STAINED_GLASS_PANE";

        @Comment("GUI item slots mapped to their configuration.\nThe key is the slot number (0-indexed from top-left, max = rows*9 - 1).\nEach item defines:\n  material - Bukkit Material name for the item's icon.\n  name     - Display name (supports MiniMessage formatting).\n  lore     - List of lore lines (supports MiniMessage formatting).\n  action   - What happens on click. Format: 'rtp:<profile-name>' to trigger an RTP profile.\n             Example: 'rtp:default-settings' triggers the Overworld RTP profile.\nItems are hidden automatically if the player lacks permission for the linked profile.")
        public Map<String, RtpGuiItemConfig> items = Map.of(
            "11", new RtpGuiItemConfig("COMPASS", "<green>Overworld RTP",
                java.util.List.of("<gray>Click to random teleport to this destination!", "", "<gray>Cooldown: <yellow>30s", "<gray>Cost: <yellow>$0.0"),
                "rtp:default-settings"),
            "13", new RtpGuiItemConfig("NETHERRACK", "<red>Nether RTP",
                java.util.List.of("<gray>Click to random teleport to the Nether!", "", "<gray>Cooldown: <yellow>30s", "<gray>Cost: <yellow>$0.0"),
                "rtp:nether-rtp"),
            "15", new RtpGuiItemConfig("ENDER_PEARL", "<light_purple>End RTP",
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
