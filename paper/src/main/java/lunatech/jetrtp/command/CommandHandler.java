package lunatech.jetrtp.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.JetRTP;
import lunatech.jetrtp.Reloadable;

/**
 * A class to handle registration of commands.
 */
public class CommandHandler implements Reloadable {
    public static final String BASE_PERM = "jetrtp.command";
    private final JetRTP plugin;

    /**
     * Instantiates the Command handler.
     *
     * @param plugin the plugin
     */
    public CommandHandler(JetRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractJetRTP plugin) {
        CommandAPI.onLoad(
            new CommandAPIPaperConfig(plugin)
                .silentLogs(true)
        );
    }

    @Override
    public void onEnable(AbstractJetRTP plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onEnable();

        // Register commands here
        new JetRTPCommand(plugin)
            .command()
            .withAliases()
            .register();

        new RtpCommand(plugin, plugin.getRtpService())
            .command()
            .register();
    }

    @Override
    public void onDisable(AbstractJetRTP plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onDisable();
    }
}