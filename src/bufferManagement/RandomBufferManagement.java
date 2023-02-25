package bufferManagement;

import core.Message;
import core.SimClock;
import routing.MessageRouter;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomBufferManagement extends BufferManagement{
    public RandomBufferManagement(MessageRouter r) {
        super(r);
    }

    @Override
    public int compareByQueueMode(Message m1, Message m2) {
        return (m1.hashCode()/2 + m2.hashCode()/2) % 3 - 1;
    }

    @Override
    public List sortByQueueMode(List list) {
        Collections.shuffle(list, new Random(SimClock.getIntTime()));
        return list;
    }
}
