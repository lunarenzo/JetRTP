package lunatech.jetrtp;

import lunatech.jetrtp.api.JetRTPAPI;
import lunatech.jetrtp.command.CommandHandler;
import lunatech.jetrtp.config.ConfigHandler;
import lunatech.jetrtp.cooldown.CooldownHandler;
import lunatech.jetrtp.database.handler.DatabaseHandler;
import lunatech.jetrtp.hook.HookManager;
import lunatech.jetrtp.listener.ListenerHandler;
import lunatech.jetrtp.messaging.MessagingHandler;
import lunatech.jetrtp.threadutil.SchedulerHandler;
import lunatech.jetrtp.translation.TranslationHandler;
import lunatech.jetrtp.updatechecker.UpdateHandler;
import lunatech.jetrtp.utility.DB;
import lunatech.jetrtp.utility.Logger;
import lunatech.jetrtp.utility.Messaging;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main class.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class JetRTP extends AbstractJetRTP {
    private static JetRTP instance;

    // Handlers/Managers
    private ConfigHandler configHandler;
    private TranslationHandler translationHandler;
    private DatabaseHandler databaseHandler;
    private MessagingHandler messagingHandler;
    private HookManager hookManager;
    private CommandHandler commandHandler;
    private ListenerHandler listenerHandler;
    private UpdateHandler updateHandler;
    private SchedulerHandler schedulerHandler;
    private CooldownHandler cooldownHandler;
    private JetRTPAPIProvider apiHandler;

    // Services
    private lunatech.jetrtp.service.SafeLocationService safeLocationService;
    private lunatech.jetrtp.service.LocationCacheService cacheService;
    private lunatech.jetrtp.service.RtpService rtpService;

    // Handlers list (defines order of load/enable/disable)
    private List<? extends Reloadable> handlers;

    @Override
    public void onLoad() {
        instance = this;

        // Register custom WorldGuard landing flag
        lunatech.jetrtp.hook.claims.WorldGuardHook.registerFlag();

        configHandler = new ConfigHandler(this);
        translationHandler = new TranslationHandler(configHandler);
        databaseHandler = DatabaseHandler.builder()
            .withConfigHandler(configHandler)
            .withLogger(getComponentLogger())
            .withMigrate(true)
            .build();
        messagingHandler = MessagingHandler.builder()
            .withLogger(getComponentLogger())
            .withName(getName())
            .build();
        hookManager = new HookManager(this);
        commandHandler = new CommandHandler(this);
        listenerHandler = new ListenerHandler(this);
        updateHandler = new UpdateHandler(this);
        schedulerHandler = new SchedulerHandler();
        cooldownHandler = new CooldownHandler();
        apiHandler = new JetRTPAPIProvider(this);

        handlers = List.of(
            configHandler,
            translationHandler,
            databaseHandler,
            messagingHandler,
            hookManager,
            commandHandler,
            listenerHandler,
            updateHandler,
            schedulerHandler,
            cooldownHandler,
            apiHandler
        );

        DB.init(databaseHandler);
        Messaging.init(messagingHandler);
        for (Reloadable handler : handlers)
            handler.onLoad(instance);
    }

    @Override
    public void onEnable() {
        // Initialize services
        lunatech.jetrtp.service.LandClaimService claimService = new lunatech.jetrtp.hook.claims.PaperClaimService(this);
        safeLocationService = new lunatech.jetrtp.service.impl.AsyncSafeLocationService(this, claimService);
        lunatech.jetrtp.service.LagService lagService = new lunatech.jetrtp.service.impl.PaperLagService();
        cacheService = new lunatech.jetrtp.service.impl.DefaultLocationCacheService(this, safeLocationService, lagService);
        lunatech.jetrtp.hook.EconomyProvider economyProvider = new lunatech.jetrtp.hook.vault.VaultEconomyProvider();
        rtpService = new lunatech.jetrtp.service.impl.DefaultRtpService(this, safeLocationService, cacheService, economyProvider);

        // Start cache refills
        cacheService.startRefillTask();

        for (Reloadable handler : handlers)
            handler.onEnable(instance);

        if (!DB.isStarted()) {
            Logger.get().warn(ColorParser.of("<yellow>Database handler failed to start. Database support has been disabled.").build());
            Bukkit.getPluginManager().disablePlugin(this);
        }

        if (!Messaging.isReady() && configHandler.getDatabaseConfig().messaging.enabled) {
            Logger.get().warn(ColorParser.of("<yellow>Messaging handler failed to start. Messaging support has been disabled.").build());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (rtpService != null) {
            rtpService.shutdown();
        }
        if (cacheService != null) {
            cacheService.shutdown();
        }
        for (Reloadable handler : handlers.reversed()) // If reverse doesn't work implement a new List with your desired disable order
            handler.onDisable(instance);
    }

    /**
     * Use to reload the entire plugin.
     */
    public void onReload() {
        onDisable();
        onLoad();
        onEnable();
    }

    @Override
    public @NotNull ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public @NotNull HookManager getHookManager() {
        return hookManager;
    }

    public @NotNull UpdateHandler getUpdateHandler() {
        return updateHandler;
    }

    public @NotNull JetRTPAPI getApiHandler() {
        return apiHandler;
    }

    @Override
    public @NotNull lunatech.jetrtp.service.SafeLocationService getSafeLocationService() {
        return safeLocationService;
    }

    @Override
    public @NotNull lunatech.jetrtp.service.LocationCacheService getCacheService() {
        return cacheService;
    }

    @Override
    public @NotNull lunatech.jetrtp.service.RtpService getRtpService() {
        return rtpService;
    }
}
