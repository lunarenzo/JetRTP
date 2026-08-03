package lunatech.jetrtp.cooldown.listener;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.Reloadable;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle registration of event listeners.
 */
@SuppressWarnings("FieldCanBeLocal")
public class ListenerHandler implements Reloadable {
    private final AbstractJetRTP plugin;
    private final List<Listener> listeners = new ArrayList<>();

    public ListenerHandler(AbstractJetRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractJetRTP plugin) {
    }

    @Override
    public void onEnable(AbstractJetRTP plugin) {
        listeners.clear();
        listeners.add(new CooldownListener(plugin));

        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    @Override
    public void onDisable(AbstractJetRTP plugin) {
    }
}
