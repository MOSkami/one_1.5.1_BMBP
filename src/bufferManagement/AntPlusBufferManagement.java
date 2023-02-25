package bufferManagement;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.SimError;
import routing.MessageRouter;
import util.Tuple;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AntPlusBufferManagement extends BufferManagement{

    public HashMap<String, Double> pheromones;
    public HashMap<String, Double> pheromonesAccumulate;
    public static double ro = 0.5;
    public AntPlusBufferManagement(MessageRouter r) {
        super(r);
        this.pheromones = new HashMap<String, Double>();
        this.pheromonesAccumulate = new HashMap<String, Double>();
        String[] str = new String[]{"pp","pc","pw",
                "cp","cc","cw",
                "wp","wc","ww",};
        for(String c:str){
            this.pheromonesAccumulate.put(c, 1.);
        }
    }

    @Override
    public int compareByQueueMode(Message m1, Message m2) {
        String key1 = m1.getFrom().getGroupId() + m1.getTo().getGroupId();
        String key2 = m2.getFrom().getGroupId() + m2.getTo().getGroupId();
        double diff = 0;
        if (pheromonesAccumulate.containsKey(key2) &&
                pheromonesAccumulate.containsKey(key1)) {
            diff = getPheromoneAccumulateValue(key2) -
                    getPheromoneAccumulateValue(key1);
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

    @Override
    public List sortByQueueMode(List list) {
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
//                                    diff = m2.getTtl() / ((double)m2.getSize()) -
//                                            m1.getTtl() / ((double)m1.getSize());
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
//                                    diff = m2.getTtl() / ((double)m2.getSize()) -
//                                            m1.getTtl() / ((double)m1.getSize());
                            diff = (1 / (double)m2.getPathLength() - 1 / (double)m1.getPathLength());
                        }
                        if(diff == 0.0f ) return 0;
                        return (diff < 0 ? -1 : 1);
                    }
                });
        return list;
    }

    public void update(){
        for (String key : pheromones.keySet()) {
            if(pheromonesAccumulate.containsKey(key)) {
                pheromonesAccumulate.put(key,
                        pheromonesAccumulate.get(key) * (1 - ro) +
                                pheromones.get(key));
            }else{
                pheromonesAccumulate.put(key,  pheromones.get(key));
            }
        }
        pheromones.clear();
    }

    public void receiveMessage(Message m, DTNHost from) {
        String key = m.getFrom().getGroupId() + m.getTo().getGroupId();
        if(pheromones.containsKey(key)) {
            pheromones.put(key,
                    pheromones.get(key) + ((m.getTtl()) /
                            ((double)m.getSize())));

        }else {
            pheromones.put(key,1 / ((m.getTtl()) /
                    ((double)m.getSize() )));
        }
    }

    public HashMap<String, Double> getPheromones() {
        return pheromones;
    }

    public void setPheromones(HashMap<String, Double> pheromones) {
        this.pheromones = pheromones;
    }

    public HashMap<String, Double> getPheromonesAccumulate() {
        return pheromonesAccumulate;
    }

    public void setPheromonesAccumulate(HashMap<String, Double> pheromonesAccumulate) {
        this.pheromonesAccumulate = pheromonesAccumulate;
    }
    public Double getPheromoneValue(String key){
        return pheromones.get(key);
    }
    public Double getPheromoneAccumulateValue(String key){
        return pheromonesAccumulate.get(key);
    }
}
