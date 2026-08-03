package lunatech.jetrtp;

import lunatech.jetrtp.api.JetRTPAPI;

class JetRTPAPIProvider extends JetRTPAPI implements Reloadable {
    private final JetRTP plugin;

    JetRTPAPIProvider(JetRTP plugin) {
        super();
        this.plugin = plugin;
        setInstance(this);
    }
}
