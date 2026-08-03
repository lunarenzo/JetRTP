package lunatech.jetrtp.service;

public interface LagService {
    /**
     * Checks if the server is currently lagging (TPS < 19.0 or MSPT > 50.0).
     *
     * @return true if the server is lagging, false otherwise
     */
    boolean isLagging();
}
