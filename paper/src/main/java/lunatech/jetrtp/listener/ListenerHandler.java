package lunatech.jetrtp.listener;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.JetRTP;
import lunatech.jetrtp.Reloadable;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle registration of event listeners.
 */
public class ListenerHandler implements Reloadable {
    private final JetRTP plugin;
    private final List<Listener> listeners = new ArrayList<>();

    /**
     * Instantiates a the Listener handler.
     *
     * @param plugin the plugin instance
     */
    public ListenerHandler(JetRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable(AbstractJetRTP plugin) {
        listeners.clear(); // Clear the list to avoid duplicate listeners when reloading the plugin
//        listeners.add(new JetRTPListener());

        // Register listeners here
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}
