package lunatech.jetrtp.config;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.Reloadable;
import lunatech.jetrtp.config.loading.ConfigLoader;
import lunatech.jetrtp.config.typeserializer.StringListSerializer;
import lunatech.jetrtp.config.typeserializer.StringObjectMapSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that generates/loads {@literal &} provides access to a configuration file.
 */
public class ConfigHandler implements Reloadable {
    private final AbstractJetRTP plugin;
    private final Path configDir;
    private final Logger logger;

    private PluginConfig cfg;
    private DatabaseConfig databaseCfg;

    private final Map<String, RtpProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, DistributionConfig> distributions = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Config handler.
     *
     * @param plugin the plugin instance
     */
    public ConfigHandler(AbstractJetRTP plugin) {
        this.plugin = plugin;
        this.configDir = plugin.getDataFolder().toPath();
        this.logger = plugin.getComponentLogger();
    }

    public ConfigHandler(AbstractJetRTP plugin, Path configDir, Logger logger) {
        this.plugin = plugin;
        this.configDir = configDir;
        this.logger = logger;
    }

    @Override
    public void onLoad(AbstractJetRTP plugin) {
        // Save default directories and files if they do not exist
        try {
            Path rtpSettingsDir = configDir.resolve("rtpSettings");
            Path distributionsDir = configDir.resolve("distributions");
            if (!Files.exists(rtpSettingsDir)) {
                Files.createDirectories(rtpSettingsDir);
            }
            if (!Files.exists(distributionsDir)) {
                Files.createDirectories(distributionsDir);
            }

            File defaultRtp = rtpSettingsDir.resolve("default-settings.yml").toFile();
            if (!defaultRtp.exists()) {
                plugin.saveResource("rtpSettings/default-settings.yml", false);
            }
            File defaultSym = distributionsDir.resolve("default-symmetric.yml").toFile();
            if (!defaultSym.exists()) {
                plugin.saveResource("distributions/default-symmetric.yml", false);
            }
            File defaultRect = distributionsDir.resolve("default-rectangle.yml").toFile();
            if (!defaultRect.exists()) {
                plugin.saveResource("distributions/default-rectangle.yml", false);
            }
        } catch (Exception e) {
            logger.error("Failed to copy default configuration resources", e);
        }

        // Load config.yml
        cfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("config.yml"))
            .withHeader("")
            .build(PluginConfig.class);

        // Load database.yml
        databaseCfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("database.yml"))
            .withHeader("")
            .withSerializer(b -> {
                b.registerExact(StringListSerializer.TYPE_TOKEN, StringListSerializer.INSTANCE)
                    .registerExact(StringObjectMapSerializer.TYPE_TOKEN, StringObjectMapSerializer.INSTANCE);
            })
            .build(DatabaseConfig.class);

        // Load rtpSettings profiles
        profiles.clear();
        File[] rtpFiles = configDir.resolve("rtpSettings").toFile().listFiles((dir, name) -> name.endsWith(".yml"));
        if (rtpFiles != null) {
            for (File f : rtpFiles) {
                String profileName = f.getName().substring(0, f.getName().length() - 4);
                RtpProfile profile = new ConfigLoader()
                    .withLogger(logger)
                    .withPath(f.toPath())
                    .build(RtpProfile.class);
                if (profile != null) {
                    profile.name = profileName;
                    profiles.put(profileName.toLowerCase(), profile);
                }
            }
        }

        // Load distributions
        distributions.clear();
        File[] distFiles = configDir.resolve("distributions").toFile().listFiles((dir, name) -> name.endsWith(".yml"));
        if (distFiles != null) {
            for (File f : distFiles) {
                String distName = f.getName().substring(0, f.getName().length() - 4);
                DistributionConfig dist = new ConfigLoader()
                    .withLogger(logger)
                    .withPath(f.toPath())
                    .build(DistributionConfig.class);
                if (dist != null) {
                    distributions.put(distName.toLowerCase(), dist);
                }
            }
        }
    }

    /**
     * Gets main config object.
     *
     * @return the config object
     */
    public PluginConfig getConfig() {
        return cfg;
    }

    /**
     * Gets database config object.
     *
     * @return the config object
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseCfg;
    }

    /**
     * Gets all loaded profiles.
     *
     * @return map of profiles
     */
    public Map<String, RtpProfile> getProfiles() {
        return profiles;
    }

    /**
     * Saves an RTP profile back to its configuration file.
     *
     * @param profile the profile to save
     */
    public void saveProfile(RtpProfile profile) {
        try {
            Path file = configDir.resolve("rtpSettings").resolve(profile.name + ".yml");
            new ConfigLoader()
                .withLogger(logger)
                .withPath(file)
                .save(profile, RtpProfile.class);
        } catch (Exception e) {
            logger.error("Failed to save RTP profile: " + profile.name, e);
        }
    }

    /**
     * Gets all loaded distributions.
     *
     * @return map of distributions
     */
    public Map<String, DistributionConfig> getDistributions() {
        return distributions;
    }
}
