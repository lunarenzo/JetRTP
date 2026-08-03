package lunatech.jetrtp.messaging.adapter.receiver;

import lunatech.jetrtp.messaging.adapter.receiver.event.MessageReceivedEvent;
import lunatech.jetrtp.messaging.message.Message;
import io.github.milkdrinkers.threadutil.Scheduler;

public class BukkitReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(Message<?> message) {
        Scheduler.sync(() -> new MessageReceivedEvent(message).callEvent()).execute();
    }
}
