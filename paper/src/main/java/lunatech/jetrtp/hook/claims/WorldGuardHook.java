package lunatech.jetrtp.hook.claims;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

public class WorldGuardHook implements ClaimHook {
    private static StateFlag customJrtpFlag = null;

    public static void registerFlag() {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("allow-jrtp-landing", true);
            registry.register(flag);
            customJrtpFlag = flag;
        } catch (FlagConflictException e) {
            com.sk89q.worldguard.protection.flags.Flag<?> existing = WorldGuard.getInstance().getFlagRegistry().get("allow-jrtp-landing");
            if (existing instanceof StateFlag stateFlag) {
                customJrtpFlag = stateFlag;
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean isInsideClaim(Location loc) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(new BukkitWorld(loc.getWorld()));
            if (manager == null) return false;

            ApplicableRegionSet set = manager.getApplicableRegions(
                BlockVector3.at(loc.getX(), loc.getY(), loc.getZ())
            );

            if (customJrtpFlag != null) {
                boolean flagStatus = set.testState(null, customJrtpFlag);
                if (!flagStatus) {
                    return true;
                }
            }

            return set.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
