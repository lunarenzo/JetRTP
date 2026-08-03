package lunatech.jetrtp.translation;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.Reloadable;
import lunatech.jetrtp.config.ConfigHandler;

public class TranslationHandler implements Reloadable {
    private final ConfigHandler configHandler;

    public TranslationHandler(ConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    @Override
    public void onEnable(AbstractJetRTP plugin) {
        Translation.load(plugin.getDataPath(), configHandler.getConfig().language);
    }
}
