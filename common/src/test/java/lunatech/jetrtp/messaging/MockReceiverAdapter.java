package lunatech.jetrtp.messaging;

import lunatech.jetrtp.event.MockEventSystem;
import lunatech.jetrtp.messaging.adapter.receiver.ReceiverAdapter;
import lunatech.jetrtp.messaging.message.Message;

public class MockReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(Message<?> message) {
        MockEventSystem.fireEvent(new MockSyncMessageEvent(message));
    }
}
