package lunatech.jetrtp.hook.miniplaceholders;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.JetRTP;
import lunatech.jetrtp.hook.AbstractHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

public class MiniPlaceholdersHook extends AbstractHook {

    private Object expansion;

    public MiniPlaceholdersHook(JetRTP plugin) {
        super(plugin);
    }

    @Override
    public void onEnable(AbstractJetRTP plugin) {
        if (!isHookLoaded()) return;

        try {
            ClassLoader cl = getClass().getClassLoader();
            Class<?> builderClass = Class.forName("io.github.miniplaceholders.api.Expansion$Builder");
            Class<?> expansionClass = Class.forName("io.github.miniplaceholders.api.Expansion");
            Class<?> audiencePlaceholderClass = Class.forName("io.github.miniplaceholders.api.placeholder.AudiencePlaceholder");

            Method builderMethod = expansionClass.getMethod("builder", String.class);
            Object builder = builderMethod.invoke(null, "jrtp");

            // Proxy instances for resolve method mapping
            Object destinationWorldProxy = Proxy.newProxyInstance(cl, new Class<?>[]{audiencePlaceholderClass}, (proxy, method, args) -> {
                if (method.getName().equals("resolve") && args[0] instanceof Player player) {
                    Location loc = getPlugin().getRtpService().getLastDestination(player.getUniqueId());
                    return loc == null ? Tag.selfClosingInserting(Component.empty()) : Tag.selfClosingInserting(Component.text(loc.getWorld().getName()));
                }
                return null;
            });

            Object coordsXProxy = Proxy.newProxyInstance(cl, new Class<?>[]{audiencePlaceholderClass}, (proxy, method, args) -> {
                if (method.getName().equals("resolve") && args[0] instanceof Player player) {
                    Location loc = getPlugin().getRtpService().getLastDestination(player.getUniqueId());
                    return loc == null ? Tag.selfClosingInserting(Component.empty()) : Tag.selfClosingInserting(Component.text(loc.getBlockX()));
                }
                return null;
            });

            Object coordsYProxy = Proxy.newProxyInstance(cl, new Class<?>[]{audiencePlaceholderClass}, (proxy, method, args) -> {
                if (method.getName().equals("resolve") && args[0] instanceof Player player) {
                    Location loc = getPlugin().getRtpService().getLastDestination(player.getUniqueId());
                    return loc == null ? Tag.selfClosingInserting(Component.empty()) : Tag.selfClosingInserting(Component.text(loc.getBlockY()));
                }
                return null;
            });

            Object coordsZProxy = Proxy.newProxyInstance(cl, new Class<?>[]{audiencePlaceholderClass}, (proxy, method, args) -> {
                if (method.getName().equals("resolve") && args[0] instanceof Player player) {
                    Location loc = getPlugin().getRtpService().getLastDestination(player.getUniqueId());
                    return loc == null ? Tag.selfClosingInserting(Component.empty()) : Tag.selfClosingInserting(Component.text(loc.getBlockZ()));
                }
                return null;
            });

            Object attemptsProxy = Proxy.newProxyInstance(cl, new Class<?>[]{audiencePlaceholderClass}, (proxy, method, args) -> {
                if (method.getName().equals("resolve") && args[0] instanceof Player player) {
                    int attempts = getPlugin().getRtpService().getLastAttempts(player.getUniqueId());
                    return Tag.selfClosingInserting(Component.text(attempts));
                }
                return null;
            });

            Method audiencePlaceholderMethod = builderClass.getMethod("audiencePlaceholder", Class.class, String.class, audiencePlaceholderClass);
            builder = audiencePlaceholderMethod.invoke(builder, Player.class, "destination_world", destinationWorldProxy);
            builder = audiencePlaceholderMethod.invoke(builder, Player.class, "coords_x", coordsXProxy);
            builder = audiencePlaceholderMethod.invoke(builder, Player.class, "coords_y", coordsYProxy);
            builder = audiencePlaceholderMethod.invoke(builder, Player.class, "coords_z", coordsZProxy);
            builder = audiencePlaceholderMethod.invoke(builder, Player.class, "attempts", attemptsProxy);

            Method buildMethod = builderClass.getMethod("build");
            expansion = buildMethod.invoke(builder);

            Method registerMethod = expansionClass.getMethod("register");
            registerMethod.invoke(expansion);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable(AbstractJetRTP plugin) {
        if (!isHookLoaded() || expansion == null) return;
        try {
            Method unregisterMethod = expansion.getClass().getMethod("unregister");
            unregisterMethod.invoke(expansion);
        } catch (Exception e) {
            e.printStackTrace();
        }
        expansion = null;
    }

    @Override
    public boolean isHookLoaded() {
        return isPluginPresent("MiniPlaceholders") && isPluginEnabled("MiniPlaceholders");
    }
}
