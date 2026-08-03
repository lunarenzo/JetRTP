package lunatech.jetrtp.command;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.JetRTP;
import lunatech.jetrtp.Reloadable;
import java.util.List;

public class CommandHandler implements Reloadable {
    public static final String BASE_PERM = "jetrtp.command";
    private final JetRTP plugin;

    public CommandHandler(JetRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractJetRTP plugin) {
        // No-op. Native command api does not need onLoad registry hooks.
    }

    @Override
    public void onEnable(AbstractJetRTP plugin) {
        // Register commands natively via LifecycleEvents
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            var registrar = commands.registrar();

            var jetRtpCmd = new JetRTPCommand(plugin);
            registrar.register(jetRtpCmd.build().build());

            var rtpCmd = new RtpCommand(plugin, plugin.getRtpService());
            // Register /rtp with its description and aliases [wild]
            registrar.register(rtpCmd.build().build(), "Teleports you to a random safe location.", List.of("wild"));
        });
    }

    @Override
    public void onDisable(AbstractJetRTP plugin) {
        // No-op. Native command api does not need manual cleanup.
    }
}