package bufferManagement;

import core.Connection;
import core.Message;
import core.SimClock;
import core.SimError;
import routing.MessageRouter;
import util.Tuple;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class FifoBufferManagement extends BufferManagement{
    public FifoBufferManagement(MessageRouter r) {
        super(r);
    }

    @Override
    public int compareByQueueMode(Message m1, Message m2) {
        double diff1 = m1.getReceiveTime() - m2.getReceiveTime();
        if (diff1 == 0) {
            return 0;
        }
        return (diff1 < 0 ? -1 : 1);
    }

    @Override
    public List sortByQueueMode(List list) {
        Collections.sort(list,
                new Comparator() {
                    /** Compares two tuples by their messages' receiving time */
                    public int compare(Object o1, Object o2) {
                        double diff;
                        Message m1, m2;

                        if (o1 instanceof Tuple) {
                            m1 = ((Tuple<Message, Connection>)o1).getKey();
                            m2 = ((Tuple<Message, Connection>)o2).getKey();
                        }
                        else if (o1 instanceof Message) {
                            m1 = (Message)o1;
                            m2 = (Message)o2;
                        }
                        else {
                            throw new SimError("Invalid type of objects in " +
                                    "the list");
                        }

                        diff = m1.getReceiveTime() - m2.getReceiveTime();
                        if (diff == 0) {
                            return 0;
                        }
                        return (diff < 0 ? -1 : 1);
                    }
                });
        return list;
    }
}
