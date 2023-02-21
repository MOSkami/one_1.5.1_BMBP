package routing;

import core.*;
import util.Tuple;

import java.util.*;

public class MyCustomRouter extends ActiveRouter{
    public static int sendQueueMode = Q_MODE_RANDOM;
    public static final int Q_MODE_ANT = 4;
    private HashMap<String, Double> pheromones;
    private HashMap<String, Double> pheromonesAccumulate;
    public static double ro = 0.5;
    public MyCustomRouter(Settings s) {
        super(s);
        this.pheromones = new HashMap<String, Double>();
        this.pheromonesAccumulate = new HashMap<String, Double>();
    }
    public MyCustomRouter(MyCustomRouter r) {
        super(r);
        this.pheromones = new HashMap<String, Double>();
        this.pheromonesAccumulate = new HashMap<String, Double>();

    }
    public HashMap<String, Double> getPheromone(){
        return this.pheromones;
    }
    public HashMap<String, Double> getPheromoneAccumulate(){
        return this.pheromones;
    }
    public Double getPheromoneValue(String key){
        return pheromones.get(key);
    }
    public Double getPheromoneAccumulateValue(String key){
        return pheromonesAccumulate.get(key);
    }
    @Override
    public void update() {
        super.update();
        for (String key : pheromones.keySet()) {
            if(pheromonesAccumulate.containsKey(key)) {
                pheromonesAccumulate.put(key, pheromonesAccumulate.get(key) * (1 - ro) + pheromones.get(key));
            }else{
                pheromonesAccumulate.put(key,  pheromones.get(key));
            }
        }
        pheromones.clear();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();
    }
    public int receiveMessage(Message m, DTNHost from) {
        String key = m.getFrom().getGroupId() + m.getTo().getGroupId();
        if(pheromones.containsKey(key)) {
            pheromones.put(key,
                    pheromones.get(key) + 1 / (double)m.getPathLength());
        }else {
            pheromones.put(key, 1 / (double)m.getPathLength());
        }
        return super.receiveMessage(m,from);
    }
    @Override
    public MyCustomRouter replicate() {
        return new MyCustomRouter(this);
    }

    public int compareByQueueMode(Message m1, Message m2) {
        switch (sendQueueMode) {
            case Q_MODE_RANDOM: {
                /* return randomly (enough) but consistently -1, 0 or 1 */
                return (m1.hashCode() / 2 + m2.hashCode() / 2) % 3 - 1;
            }
            case Q_MODE_FIFO:{
                double diff1 = m1.getReceiveTime() - m2.getReceiveTime();
                if (diff1 == 0) {
                    return 0;
                }
                return (diff1 < 0 ? -1 : 1);
            }
            case Q_MODE_HOPS: {
                double diff2 = m1.getPathLength() - m2.getPathLength();
                if (diff2 == 0) {
                    return 0;
                }
                return (diff2 < 0 ? -1 : 1);
            }
            case Q_MODE_ANT: {
                String key1 = m1.getFrom().getGroupId() + m1.getTo().getGroupId();
                String key2 = m2.getFrom().getGroupId() + m2.getTo().getGroupId();
                double diff = 0;
                if (this.pheromonesAccumulate.containsKey(key2) &&
                        this.pheromonesAccumulate.containsKey(key1)) {
                    diff = this.getPheromoneAccumulateValue(key2) -
                            this.getPheromoneAccumulateValue(key1);
                } else if (pheromonesAccumulate.containsKey(key2)) {
                    return -1;
                } else if (pheromonesAccumulate.containsKey(key1)) {
                    return 1;
                } else {
                    return 0;
                }
                if (diff == 0.0f) return 0;
                return (diff < 0 ? -1 : 1);
            }
            default:
                throw new SimError("Unknown queue mode " + sendQueueMode);
        }
    }

    public List sortByQueueMode(List list) {
        switch (sendQueueMode) {
            case Q_MODE_RANDOM: {
                Collections.shuffle(list, new Random(SimClock.getIntTime()));
                break;
            }
            case Q_MODE_FIFO: {
                Collections.sort(list,
                        new Comparator() {
                            /**
                             * Compares two tuples by their messages' receiving time
                             */
                            public int compare(Object o1, Object o2) {
                                double diff;
                                Message m1, m2;

                                if (o1 instanceof Tuple) {
                                    m1 = ((Tuple<Message, Connection>) o1).getKey();
                                    m2 = ((Tuple<Message, Connection>) o2).getKey();
                                } else if (o1 instanceof Message) {
                                    m1 = (Message) o1;
                                    m2 = (Message) o2;
                                } else {
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
                break;
            }
            case Q_MODE_HOPS:{
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

                                diff = m1.getPathLength() - m2.getPathLength();
                                if (diff == 0) {
                                    return 0;
                                }
                                return (diff < 0 ? -1 : 1);
                            }
                        });
                break;
            }
            case Q_MODE_ANT:{
                Collections.sort(list,
                        new Comparator() {
                            /** Compares two tuples by their messages' receiving time */
                            public int compare(Object o1, Object o2) {
                                double diff = 0;
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
                                String key1 = m1.getFrom().getGroupId() + m1.getTo().getGroupId();
                                String key2 = m2.getFrom().getGroupId() + m2.getTo().getGroupId();
                                if(key1.equals(key2)){
                                    diff = m1.getPathLength() - m2.getPathLength();
                                    if(diff == 0) return 0;
                                    return (diff < 0 ? -1 : 1);
                                }
                                if(pheromonesAccumulate.containsKey(key2) &&
                                        pheromonesAccumulate.containsKey(key1)) {
                                    diff = getPheromoneAccumulateValue(key2) -
                                            getPheromoneAccumulateValue(key1);
                                }
                                else if (pheromonesAccumulate.containsKey(key2)) {
                                    return -1;
                                }else if (pheromonesAccumulate.containsKey(key1)){
                                    return 1;
                                }else{
                                    return (int)(1 / (double)m1.getPathLength() - 1 / (double)m2.getPathLength());
                                }
                                if(diff == 0.0f ) return 0;
                                return (diff < 0 ? -1 : 1);
                            }
                        });
            }
            default:
                throw new SimError("Unknown queue mode " + sendQueueMode);
        }
        return list;
    }
}
