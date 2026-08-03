package lunatech.jetrtp.utility;


import lunatech.jetrtp.AbstractJetRTP;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

/**
 * A class that provides shorthand access to {@link AbstractJetRTP#getComponentLogger}.
 */
public class Logger {
    /**
     * Get component logger. Shorthand for:
     *
     * @return the component logger {@link AbstractJetRTP#getComponentLogger}.
     */
    @NotNull
    public static ComponentLogger get() {
        return AbstractJetRTP.getInstance().getComponentLogger();
    }
}
