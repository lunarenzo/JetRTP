package lunatech.jetrtp.event;

@FunctionalInterface
public interface MockEventListener {
    void onEvent(MockEvent event);
}