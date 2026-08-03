package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.hook.EconomyProvider;
import lunatech.jetrtp.service.LocationCacheService;
import lunatech.jetrtp.service.RtpService;
import lunatech.jetrtp.service.SafeLocationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultRtpService implements RtpService {

    private final AbstractJetRTP plugin;
    private final SafeLocationService safeLocationService;
    private final LocationCacheService cacheService;
    private final EconomyProvider economyProvider;

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> warmupTasks = new ConcurrentHashMap<>();
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

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private final long startTime = System.currentTimeMillis();
            private int lastCountdownValue = -1;

            @Override
            public void run() {
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
                        String title = profile.warmup.titleCountdown.replace('&', '§');
                        String subtitle = profile.warmup.subtitleCountdown.replace('&', '§').replace("%time%", String.valueOf(remaining));
                        player.sendTitle(title, subtitle, 0, 20, 5);

                        try {
                            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(profile.warmup.soundCountdown.toUpperCase());
                            float pitch = 1.0f;
                            if (profile.warmup.soundCountdownPitchIncrease) {
                                pitch = 0.8f + (float) (profile.warmup.time - remaining) * 0.15f;
                            }
                            player.playSound(player.getLocation(), sound, 0.6f, pitch);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }, 2L, 20L);

        warmupTasks.put(uuid, taskId);
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
        cooldowns.put(player.getName().toLowerCase() + ":" + profile.name.toLowerCase(), System.currentTimeMillis());

        if (profile.cost > 0 && economyProvider.hasEconomy()) {
            economyProvider.withdraw(player, profile.cost);
        }

        String title = profile.warmup.titleSuccess.replace('&', '§');
        String subtitle = profile.warmup.subtitleSuccess.replace('&', '§');
        player.sendTitle(title, subtitle, 5, 45, 15);

        for (String soundStr : profile.warmup.soundsSuccess) {
            try {
                String[] parts = soundStr.split(":");
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[0].toUpperCase());
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
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
        Integer taskId = warmupTasks.remove(uuid);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        warmupStartLocations.remove(uuid);
    }

    @Override
    public boolean isOnCooldown(Player player, RtpProfile profile) {
        if (player.hasPermission("jakesrtp.nocooldown") || player.hasPermission("jakesrtp.nocooldown." + profile.name.toLowerCase())) {
            return false;
        }
        Long lastUse = cooldowns.get(player.getName().toLowerCase() + ":" + profile.name.toLowerCase());
        if (lastUse == null) {
            return false;
        }
        return System.currentTimeMillis() < lastUse + (profile.cooldown * 1000L);
    }

    @Override
    public long getRemainingCooldown(Player player, RtpProfile profile) {
        Long lastUse = cooldowns.get(player.getName().toLowerCase() + ":" + profile.name.toLowerCase());
        if (lastUse == null) {
            return 0;
        }
        long diff = (lastUse + (profile.cooldown * 1000L)) - System.currentTimeMillis();
        return Math.max(0, diff);
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
                String title = profile.warmup.titleCancel.replace('&', '§');
                String subtitle = profile.warmup.subtitleCancel.replace('&', '§');
                player.sendTitle(title, subtitle, 5, 40, 15);

                for (String soundStr : profile.warmup.soundsCancel) {
                    try {
                        String[] parts = soundStr.split(":");
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[0].toUpperCase());
                        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                        player.playSound(player.getLocation(), sound, volume, pitch);
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
        cooldowns.clear();
    }
}
