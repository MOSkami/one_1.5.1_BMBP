package bufferManagement;

import core.Connection;
import core.Message;
import core.SimError;
import routing.MessageRouter;
import util.Tuple;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TtlBufferManagement extends BufferManagement{
    public TtlBufferManagement(MessageRouter r) {
        super(r);
    }

    @Override
    public int compareByQueueMode(Message m1, Message m2) {
        double diff2 = m1.getPathLength() - m2.getPathLength();
        if (diff2 == 0) {
            return 0;
        }
        return (diff2 < 0 ? -1 : 1);
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

                        diff = m1.getTtl() - m2.getTtl();
                        if (diff == 0) {
                            return 0;
                        }
                        return (diff < 0 ? -1 : 1);
                    }
                });
        return list;
    }
}
