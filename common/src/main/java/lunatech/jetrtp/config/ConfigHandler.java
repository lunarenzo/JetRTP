package lunatech.jetrtp.config;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.Reloadable;
import lunatech.jetrtp.config.loading.ConfigLoader;
import lunatech.jetrtp.config.typeserializer.StringListSerializer;
import lunatech.jetrtp.config.typeserializer.StringObjectMapSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * A class that generates/loads {@literal &} provides access to a configuration file.
 */
public class ConfigHandler implements Reloadable {
    private final AbstractJetRTP plugin;
    private final Path configDir;
    private final Logger logger;

    private PluginConfig cfg;
    private DatabaseConfig databaseCfg;

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
        cfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("config.yml"))
            .withHeader("")
            .build(PluginConfig.class);

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
}
