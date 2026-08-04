package lunatech.jetrtp.hook.placeholderapi;

import lunatech.jetrtp.JetRTP;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A PlaceholderAPI expansion. Read the docs at <a href="https://wiki.placeholderapi.com/developers/creating-a-placeholderexpansion/">here</a> on how to register your custom placeholders.
 */
public class PAPIExpansion extends PlaceholderExpansion {
    private final JetRTP plugin;

    public PAPIExpansion(JetRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jrtp";
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This needs to be true, or PlaceholderAPI will unregister the expansion during a plugin reload.
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer p, @NotNull String params) {
        if (p == null) return null;
        org.bukkit.Location loc = plugin.getRtpService().getLastDestination(p.getUniqueId());
        if (loc == null) return "";

        return switch (params.toLowerCase()) {
            case "destination_world" -> loc.getWorld().getName();
            case "coords_x" -> String.valueOf(loc.getBlockX());
            case "coords_y" -> String.valueOf(loc.getBlockY());
            case "coords_z" -> String.valueOf(loc.getBlockZ());
            default -> null;
        };
    }
}
