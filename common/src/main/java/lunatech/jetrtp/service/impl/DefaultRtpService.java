package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.cooldown.CooldownType;
import lunatech.jetrtp.cooldown.Cooldowns;
import lunatech.jetrtp.hook.EconomyProvider;
import lunatech.jetrtp.service.LocationCacheService;
import lunatech.jetrtp.service.RtpService;
import lunatech.jetrtp.service.SafeLocationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultRtpService implements RtpService {

    private final AbstractJetRTP plugin;
    private final SafeLocationService safeLocationService;
    private final LocationCacheService cacheService;
    private final EconomyProvider economyProvider;

    private final Map<UUID, ScheduledTask> warmupTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> warmupStartLocations = new ConcurrentHashMap<>();

    public DefaultRtpService(
        AbstractJetRTP plugin,
        SafeLocationService safeLocationService,
        LocationCacheService cacheService,
        EconomyProvider economyProvider
    ) {
        this.plugin = plugin;
        this.safeLocationService = safeLocationService;
        this.cacheService = cacheService;
        this.economyProvider = economyProvider;
    }

    @Override
    public CompletableFuture<Boolean> executeRtp(Player player, RtpProfile profile) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (isOnCooldown(player, profile)) {
            future.complete(false);
            return future;
        }

        if (profile.cost > 0 && economyProvider.hasEconomy() && economyProvider.getBalance(player) < profile.cost) {
            future.complete(false);
            return future;
        }

        boolean hasWarmup = profile.warmup.time > 0
            && !player.hasPermission("jakesrtp.nowarmup")
            && !player.hasPermission("jakesrtp.nowarmup." + profile.name.toLowerCase());

        if (hasWarmup) {
            startWarmup(player, profile, future);
        } else {
            performTeleportation(player, profile, future);
        }

        return future;
    }

    private void startWarmup(Player player, RtpProfile profile, CompletableFuture<Boolean> future) {
        if (hasActiveWarmup(player)) {
            future.complete(false);
            return;
        }

        UUID uuid = player.getUniqueId();
        warmupStartLocations.put(uuid, player.getLocation().clone());

        ScheduledTask task = player.getScheduler().runAtFixedRate(
            plugin,
            new java.util.function.Consumer<ScheduledTask>() {
                private final long startTime = System.currentTimeMillis();
                private int lastCountdownValue = -1;

                @Override
                public void accept(ScheduledTask scheduledTask) {
                    if (!player.isOnline()) {
                        cancelWarmupTask(uuid);
                        future.complete(false);
                        return;
                    }

                    if (profile.warmup.cancelOnMove) {
                        Location startLoc = warmupStartLocations.get(uuid);
                        if (startLoc != null && (!startLoc.getWorld().equals(player.getWorld()) || startLoc.distanceSquared(player.getLocation()) > 1.0)) {
                            cancelWarmup(player);
                            future.complete(false);
                            return;
                        }
                    }

                    int elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000L);
                    int remaining = profile.warmup.time - elapsed;

                    if (remaining <= 0) {
                        cancelWarmupTask(uuid);
                        performTeleportation(player, profile, future);
                    } else {
                        if (profile.warmup.countDown && remaining != lastCountdownValue) {
                            lastCountdownValue = remaining;
                            showTitle(player, profile.warmup.titleCountdown, profile.warmup.subtitleCountdown.replace("%time%", String.valueOf(remaining)), 0, 20, 5);

                            try {
                                org.bukkit.Sound sound = getSound(profile.warmup.soundCountdown);
                                if (sound != null) {
                                    float pitch = 1.0f;
                                    if (profile.warmup.soundCountdownPitchIncrease) {
                                        pitch = 0.8f + (float) (profile.warmup.time - remaining) * 0.15f;
                                    }
                                    player.playSound(player.getLocation(), sound, 0.6f, pitch);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            },
            () -> {
                // retired callback
                cancelWarmupTask(uuid);
            },
            2L,
            20L
        );

        warmupTasks.put(uuid, task);
    }

    private void performTeleportation(Player player, RtpProfile profile, CompletableFuture<Boolean> future) {
        if (profile.cost > 0 && economyProvider.hasEconomy() && economyProvider.getBalance(player) < profile.cost) {
            future.complete(false);
            return;
        }

        Location cachedLoc = cacheService.popCachedLocation(profile);
        if (cachedLoc != null) {
            teleportPlayer(player, profile, cachedLoc, future);
        } else {
            safeLocationService.findSafeLocationAsync(profile, player.getLocation()).thenAccept(loc -> {
                teleportPlayer(player, profile, loc, future);
            }).exceptionally(ex -> {
                future.complete(false);
                return null;
            });
        }
    }

    private void teleportPlayer(Player player, RtpProfile profile, Location loc, CompletableFuture<Boolean> future) {
        if (profile.preferSyncTp.command) {
            player.teleport(loc);
            completeTeleportation(player, profile, loc, future);
        } else {
            player.teleportAsync(loc).thenAccept(success -> {
                if (success) {
                    completeTeleportation(player, profile, loc, future);
                } else {
                    future.complete(false);
                }
            });
        }
    }

    private void completeTeleportation(Player player, RtpProfile profile, Location loc, CompletableFuture<Boolean> future) {
        Cooldowns.set(player.getUniqueId(), CooldownType.RTP_COOLDOWN, java.time.Duration.ofSeconds(profile.cooldown));

        if (profile.cost > 0 && economyProvider.hasEconomy()) {
            economyProvider.withdraw(player, profile.cost);
        }

        showTitle(player, profile.warmup.titleSuccess, profile.warmup.subtitleSuccess, 5, 45, 15);

        for (String soundStr : profile.warmup.soundsSuccess) {
            try {
                String[] parts = soundStr.split(":");
                org.bukkit.Sound sound = getSound(parts[0]);
                if (sound != null) {
                    float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            } catch (Exception ignored) {}
        }

        for (String cmd : profile.thenExecute) {
            String filled = cmd
                .replace("%player%", player.getName())
                .replace("%PLAYER%", player.getName())
                .replace("%location%", loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")")
                .replace("%world%", loc.getWorld().getName())
                .replace("%x%", String.valueOf(loc.getBlockX()))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ()));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), filled);
        }

        future.complete(true);
    }

    private void cancelWarmupTask(UUID uuid) {
        ScheduledTask task = warmupTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        warmupStartLocations.remove(uuid);
    }

    @Override
    public boolean isOnCooldown(Player player, RtpProfile profile) {
        if (player.hasPermission("jakesrtp.nocooldown") || player.hasPermission("jakesrtp.nocooldown." + profile.name.toLowerCase())) {
            return false;
        }
        return Cooldowns.has(player.getUniqueId(), CooldownType.RTP_COOLDOWN);
    }

    @Override
    public long getRemainingCooldown(Player player, RtpProfile profile) {
        return Cooldowns.getRemaining(player.getUniqueId(), CooldownType.RTP_COOLDOWN).toMillis();
    }

    @Override
    public void startWarmup(Player player, RtpProfile profile) {
        executeRtp(player, profile);
    }

    @Override
    public void cancelWarmup(Player player) {
        UUID uuid = player.getUniqueId();
        if (warmupTasks.containsKey(uuid)) {
            cancelWarmupTask(uuid);
            RtpProfile profile = plugin.getConfigHandler().getProfiles().values().stream().findFirst().orElse(null);
            if (profile != null) {
                showTitle(player, profile.warmup.titleCancel, profile.warmup.subtitleCancel, 5, 40, 15);

                for (String soundStr : profile.warmup.soundsCancel) {
                    try {
                        String[] parts = soundStr.split(":");
                        org.bukkit.Sound sound = getSound(parts[0]);
                        if (sound != null) {
                            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                            player.playSound(player.getLocation(), sound, volume, pitch);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public boolean hasActiveWarmup(Player player) {
        return warmupTasks.containsKey(player.getUniqueId());
    }

    @Override
    public void shutdown() {
        for (UUID uuid : warmupTasks.keySet()) {
            cancelWarmupTask(uuid);
        }
    }

    private void showTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(
            java.time.Duration.ofMillis(fadeInTicks * 50L),
            java.time.Duration.ofMillis(stayTicks * 50L),
            java.time.Duration.ofMillis(fadeOutTicks * 50L)
        );
        player.showTitle(net.kyori.adventure.title.Title.title(
            io.github.milkdrinkers.colorparser.paper.ColorParser.of(title).build(),
            io.github.milkdrinkers.colorparser.paper.ColorParser.of(subtitle).build(),
            times
        ));
    }

    @SuppressWarnings("removal")
    private org.bukkit.Sound getSound(String soundName) {
        if (soundName == null) return null;
        try {
            org.bukkit.NamespacedKey key = soundName.contains(":") 
                ? org.bukkit.NamespacedKey.fromString(soundName.toLowerCase()) 
                : org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase().replace("_", "."));
            if (key != null) {
                org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(key);
                if (sound != null) return sound;
            }
        } catch (Exception ignored) {}
        try {
            return org.bukkit.Sound.valueOf(soundName.toUpperCase());
        } catch (Exception ignored) {}
        return null;
    }
}
