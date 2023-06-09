package bufferManagement;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.SimError;
import routing.MessageRouter;
import util.Tuple;

import java.util.*;

public class AntBufferManagement extends BufferManagement{

    public HashMap<String, Double> pheromones;
    public HashMap<String, Double> pheromonesAccumulate;
    public static double ro = 0.5;
    public AntBufferManagement(MessageRouter r) {
        super(r);
        this.pheromones = new HashMap<String, Double>();
        this.pheromonesAccumulate = new HashMap<String, Double>();
        int groupNum = Integer.parseInt(r.getSettings().getSettingFullPropName("Scenario.nrofHostGroups"));
        List<String> strs = new ArrayList<String>();
        for(int i = 1;i <= groupNum;i++){
            for(int j = 1;j <= groupNum;j++){
                String left = r.getSettings().getSettingFullPropName("Group"+i+".groupID");
                String right = r.getSettings().getSettingFullPropName("Group"+j+".groupID");
                strs.add(left+right);
            }
        }
        for(String c:strs){
            this.pheromonesAccumulate.put(c, Double.parseDouble(r.getSettings().getSetting("Ant.initialPheromone")));
        }
    }

    @Override
    public int compareByQueueMode(Message m1, Message m2) {
        String key1 = m1.getFrom().getGroupId() + m1.getTo().getGroupId();
        String key2 = m2.getFrom().getGroupId() + m2.getTo().getGroupId();
        double diff = 0;
        if (pheromonesAccumulate.containsKey(key2) &&
                pheromonesAccumulate.containsKey(key1)) {
            diff = this.getPheromoneAccumulateValue(key2) -
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
                            diff = (1 / (double)m2.getPathLength() - 1 / (double)m1.getPathLength());
                        }
                        if(diff == 0.0f ) return 0;
                        return (diff < 0 ? -1 : 1);
                    }
                });
        return list;
    }


    public void receiveMessage(Message m, DTNHost from) {
        String key = m.getFrom().getGroupId() + m.getTo().getGroupId();
        if(pheromones.containsKey(key)) {
            pheromones.put(key,
                    pheromones.get(key) + 1 / (double) m.getPathLength());

        }else {
            pheromones.put(key,1 / (double) m.getPathLength());
        }
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
