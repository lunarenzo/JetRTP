package lunatech.jetrtp.hook;

import org.bukkit.entity.Player;

public interface EconomyProvider {
    /**
     * Checks if the economy system is active.
     */
    boolean hasEconomy();

    /**
     * Gets the player's balance.
     */
    double getBalance(Player player);

    /**
     * Withdraws money from the player.
     *
     * @return true if successful, false otherwise
     */
    boolean withdraw(Player player, double amount);

    /**
     * Formats a double amount into a readable currency string.
     */
    String format(double amount);
}
