package lunatech.jetrtp.hook.vault;

import lunatech.jetrtp.hook.EconomyProvider;
import lunatech.jetrtp.hook.Hook;
import org.bukkit.entity.Player;

public class VaultEconomyProvider implements EconomyProvider {
    @Override
    public boolean hasEconomy() {
        return Hook.Vault.isLoaded() && ((VaultHook) Hook.Vault.get()).isEconomyLoaded();
    }

    @Override
    public double getBalance(Player player) {
        if (hasEconomy()) {
            return ((VaultHook) Hook.Vault.get()).getEconomy().getBalance(player);
        }
        return 0.0;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (hasEconomy()) {
            return ((VaultHook) Hook.Vault.get()).getEconomy().withdrawPlayer(player, amount).transactionSuccess();
        }
        return true;
    }

    @Override
    public String format(double amount) {
        if (hasEconomy()) {
            return ((VaultHook) Hook.Vault.get()).getEconomy().format(amount);
        }
        return String.valueOf(amount);
    }
}
