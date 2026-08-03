package lunatech.jetrtp.messaging;

import lunatech.jetrtp.event.MockEvent;
import lunatech.jetrtp.messaging.message.Message;

public class MockSyncMessageEvent extends MockEvent {
    private final Message<?> message;

    public MockSyncMessageEvent(Message<?> message) {
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }
}