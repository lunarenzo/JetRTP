package lunatech.jetrtp.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * The JetRTPAPI class is the main entry point for accessing the JetRTP API.
 */
public abstract class JetRTPAPI {
    private static JetRTPAPI INSTANCE;

    /**
     * Gets the instance of the JetRTPAPI.
     *
     * @return the instance of JetRTPAPI
     * @since 1.0.0
     */
    public static JetRTPAPI getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("API was accessed before being initialized!");
        return INSTANCE;
    }

    /**
     * Sets the instance of the JetRTPAPI.
     * This method is intended for internal use by the api provider only.
     *
     * @param api the instance of JetRTPAPI to set
     * @since 1.0.0
     */
    @ApiStatus.Internal
    protected static void setInstance(JetRTPAPI api) {
        INSTANCE = api;
    }
}
