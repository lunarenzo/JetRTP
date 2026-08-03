package lunatech.jetrtp;

/**
 * Implemented in classes that should support being reloaded IE executing the methods during runtime after startup.
 */
public interface Reloadable {
    /**
     * On plugin load.
     */
    default void onLoad(AbstractJetRTP plugin) {
    }

    /**
     * On plugin enable.
     */
    default void onEnable(AbstractJetRTP plugin) {
    }

    /**
     * On plugin disable.
     */
    default void onDisable(AbstractJetRTP plugin) {
    }

}
