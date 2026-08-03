package lunatech.jetrtp.utility;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.ConfigHandler;
import lunatech.jetrtp.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Convenience class for accessing {@link ConfigHandler#getConfig}
 */
public final class Cfg {
    /**
     * Convenience method for {@link ConfigHandler#getConfig} to getConnection {@link PluginConfig}
     *
     * @return the config
     */
    @NotNull
    public static PluginConfig get() {
        return AbstractJetRTP.getInstance().getConfigHandler().getConfig();
    }
}
