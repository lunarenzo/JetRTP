package lunatech.jetrtp.service.impl;

import lunatech.jetrtp.service.LagService;
import org.bukkit.Bukkit;

public class PaperLagService implements LagService {
    @Override
    public boolean isLagging() {
        try {
            double tickTime = Bukkit.getAverageTickTime();
            if (tickTime > 50.0) {
                return true;
            }
        } catch (Throwable ignored) {
            try {
                double tps = Bukkit.getTPS()[0];
                if (tps < 19.0) {
                    return true;
                }
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }
}
