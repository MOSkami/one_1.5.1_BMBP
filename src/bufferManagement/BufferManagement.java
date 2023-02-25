package bufferManagement;

import core.DTNHost;
import core.Message;
import core.SimClock;
import routing.MessageRouter;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class BufferManagement implements Cloneable{

    private MessageRouter router;

    public MessageRouter getRouter() {
        return router;
    }

    public void setRouter(MessageRouter router) {
        this.router = router;
    }

    public BufferManagement(MessageRouter r){
        this.router = r;
    }
    public abstract int compareByQueueMode(Message m1, Message m2);

    public abstract List sortByQueueMode(List list);
    public BufferManagement clone() throws CloneNotSupportedException {
        return (BufferManagement) super.clone();
    }

    public void receiveMessage(Message m, DTNHost from) {

    }

    public void update(){

    }
}
