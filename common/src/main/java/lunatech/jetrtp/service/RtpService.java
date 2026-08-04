package lunatech.jetrtp.service;

import lunatech.jetrtp.config.RtpProfile;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;

public interface RtpService {
    /**
     * Executes the RTP process for a player. Handles warmups, charges, and coordinate checks.
     *
     * @param player the player to teleport
     * @param profile the settings profile to use
     * @return a future completing with true if teleported, false otherwise
     */
    CompletableFuture<Boolean> executeRtp(Player player, RtpProfile profile);

    /**
     * Checks if the player is currently on cooldown for the profile.
     *
     * @param player the player
     * @param profile the settings profile
     * @return true if on cooldown, false otherwise
     */
    boolean isOnCooldown(Player player, RtpProfile profile);

    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @param player the player
     * @param profile the settings profile
     * @return remaining time in millis
     */
    long getRemainingCooldown(Player player, RtpProfile profile);

    /**
     * Starts the teleportation warmup for a player.
     *
     * @param player the player
     * @param profile the settings profile
     */
    void startWarmup(Player player, RtpProfile profile);

    /**
     * Cancels any active warmup for a player.
     *
     * @param player the player
     */
    void cancelWarmup(Player player);

    /**
     * Checks if the player has an active warmup scheduled.
     *
     * @param player the player
     * @return true if warmup is active, false otherwise
     */
    boolean hasActiveWarmup(Player player);

    /**
     * Checks if there are any active warmups on the server.
     *
     * @return true if there is at least one active warmup, false otherwise
     */
    boolean hasAnyActiveWarmups();

    /**
     * Shuts down active warmups and clears state.
     */
    void shutdown();

    /**
     * Gets the last teleportation destination for the player.
     *
     * @param uuid the player's UUID
     * @return the last destination location, or null if they haven't teleported yet
     */
    org.bukkit.Location getLastDestination(java.util.UUID uuid);

    /**
     * Gets the max attempts of the last teleport attempt for the player.
     *
     * @param uuid the player's UUID
     * @return the last max attempts count, or 0 if they haven't teleported yet
     */
    int getLastAttempts(java.util.UUID uuid);
}
