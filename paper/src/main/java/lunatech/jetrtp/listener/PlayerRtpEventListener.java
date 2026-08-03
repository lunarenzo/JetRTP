package lunatech.jetrtp.listener;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.PluginConfig;
import lunatech.jetrtp.config.RtpProfile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRtpEventListener implements Listener {

    private final AbstractJetRTP plugin;

    public PlayerRtpEventListener(AbstractJetRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PluginConfig cfg = plugin.getConfigHandler().getConfig();

        if (!cfg.rtpOnFirstJoin.enabled || player.hasPlayedBefore() || player.hasPermission("jakesrtp.nofirstjoinrtp")) {
            return;
        }

        RtpProfile profile = plugin.getConfigHandler().getProfiles().get(cfg.rtpOnFirstJoin.settings.toLowerCase());
        if (profile == null) {
            return;
        }

        if (cfg.logging.rtpOnPlayerJoin) {
            plugin.getComponentLogger().info("Rtp-on-first-join triggered for " + player.getName());
        }

        plugin.getRtpService().executeRtp(player, profile).thenAccept(success -> {
            if (!success && cfg.logging.rtpOnPlayerJoin) {
                plugin.getComponentLogger().warn("Rtp-on-first-join failed for " + player.getName());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PluginConfig cfg = plugin.getConfigHandler().getConfig();

        if (!cfg.rtpOnDeath.enabled) {
            return;
        }

        if (cfg.rtpOnDeath.respectBeds && event.isBedSpawn()) {
            return;
        }

        if (cfg.rtpOnDeath.respectAnchors && event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.RESPAWN_ANCHOR) {
            return;
        }

        if (cfg.rtpOnDeath.requirePermission && !player.hasPermission("jakesrtp.rtpondeath")) {
            return;
        }

        RtpProfile profile = plugin.getConfigHandler().getProfiles().get(cfg.rtpOnDeath.settings.toLowerCase());
        if (profile == null) {
            return;
        }

        if (cfg.logging.rtpOnRespawn) {
            plugin.getComponentLogger().info("Rtp-on-respawn triggered for " + player.getName());
        }

        // Try popping location from cache instantly to prevent main-thread block
        Location cachedLoc = plugin.getCacheService().popCachedLocation(profile);
        if (cachedLoc != null) {
            event.setRespawnLocation(cachedLoc);
        } else {
            // Find safe location asynchronously and teleport player shortly after respawn
            plugin.getSafeLocationService().findSafeLocationAsync(profile, player.getLocation())
                .thenAccept(loc -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.teleportAsync(loc);
                        }
                    });
                });
        }
    }
}
