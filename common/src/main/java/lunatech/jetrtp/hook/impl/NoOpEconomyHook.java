package lunatech.jetrtp.hook.impl;

import lunatech.jetrtp.hook.EconomyProvider;
import org.bukkit.entity.Player;

public class NoOpEconomyHook implements EconomyProvider {
    @Override
    public boolean hasEconomy() {
        return false;
    }

    @Override
    public double getBalance(Player player) {
        return 0.0;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return true; // No-op withdraw always succeeds
    }

    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}
