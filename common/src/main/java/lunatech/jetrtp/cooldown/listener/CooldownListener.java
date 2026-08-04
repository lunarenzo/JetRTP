package lunatech.jetrtp.cooldown.listener;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.cooldown.Cooldowns;
import lunatech.jetrtp.cooldown.CooldownType;
import lunatech.jetrtp.database.Queries;
import io.github.milkdrinkers.threadutil.Scheduler;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;

@SuppressWarnings({"unused", "FieldCanBeLocal", "CodeBlock2Expr"})
class CooldownListener implements Listener {
    private final AbstractJetRTP plugin;

    public CooldownListener(AbstractJetRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final var player = e.getPlayer();
        final String storageType = plugin.getConfigHandler().getConfig().cooldownStorageType;

        if ("DATABASE".equalsIgnoreCase(storageType)) {
            Scheduler.async(() -> {
                Queries.Cooldown.load(player).forEach((cooldownType, instant) -> {
                    Cooldowns.set(player, cooldownType, instant);
                });
            }).execute();
        } else {
            final PersistentDataContainer pdc = player.getPersistentDataContainer();
            for (CooldownType type : CooldownType.values()) {
                final NamespacedKey key = new NamespacedKey(plugin, "cooldown_" + type.name().toLowerCase());
                if (pdc.has(key, PersistentDataType.LONG)) {
                    final Long expireTime = pdc.get(key, PersistentDataType.LONG);
                    if (expireTime != null) {
                        Cooldowns.set(player, type, Instant.ofEpochMilli(expireTime));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        final var player = e.getPlayer();
        final String storageType = plugin.getConfigHandler().getConfig().cooldownStorageType;

        if ("DATABASE".equalsIgnoreCase(storageType)) {
            Scheduler.async(() -> {
                Queries.Cooldown.save(player);
                Cooldowns.removeAll(player);
            }).execute();
        } else {
            final PersistentDataContainer pdc = player.getPersistentDataContainer();
            for (CooldownType type : CooldownType.values()) {
                final NamespacedKey key = new NamespacedKey(plugin, "cooldown_" + type.name().toLowerCase());
                if (Cooldowns.has(player, type)) {
                    final Instant expires = Cooldowns.get(player, type);
                    if (expires != null) {
                        pdc.set(key, PersistentDataType.LONG, expires.toEpochMilli());
                    }
                } else {
                    pdc.remove(key);
                }
            }
            Cooldowns.removeAll(player);
        }
    }
}
