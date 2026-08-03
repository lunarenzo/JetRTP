package lunatech.jetrtp;

import lunatech.jetrtp.config.ConfigHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractJetRTP extends JavaPlugin {
    private static AbstractJetRTP instance;

    /**
     * Gets plugin instance.
     *
     * @return the plugin instance
     */
    public static AbstractJetRTP getInstance() {
        return AbstractJetRTP.instance;
    }

    AbstractJetRTP() {
        AbstractJetRTP.instance = this;
    }

    /**
     * Gets config handler.
     *
     * @return the config handler
     */
    public abstract @NotNull ConfigHandler getConfigHandler();
}
