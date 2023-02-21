package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author roardragon
 * @since  2013-05-11
 * @version 1.0
 * 
 */
public class NetworkOverheadReport extends Report implements MessageListener{

    public static final String Header="#the average network overhead for each message during the whole simulation";

    public static final String ColumnNames="from to hops";

    protected Map<Message,Integer> avgHops;

    protected Integer prevHops;
    protected double bufferUsage;

    public NetworkOverheadReport(){
        init();
    }
    
    @Override
    protected void init(){
        super.init();
        avgHops=new HashMap<Message,Integer>();
        prevHops=0;
        bufferUsage=0;
    }
    
    @Override
    public void done(){
        if(avgHops.isEmpty()){
            write("#There is no message transferred");
        }
        else{
            String resultMsg = Header+"\n"+ColumnNames+'\n';
            Set<Map.Entry<Message,Integer>>set=avgHops.entrySet();
            double pathLength_sum = 0;
            int count = 0;
            for(Iterator<Map.Entry<Message,Integer>> it=set.iterator();it.hasNext();){
                Map.Entry<Message,Integer> entry=(Map.Entry<Message,Integer>)it.next();
                resultMsg+=String.format("%4d",entry.getKey().getFrom().getAddress())+" "
                        +String.format("%4d",entry.getKey().getTo().getAddress())+" "
                        +format(entry.getValue())+"\n";
                pathLength_sum += entry.getValue();
                count ++;
            }
            resultMsg+="\\"+" "
                    +"\\"+" "
                    +format(pathLength_sum / count)+" ";
            write(resultMsg);
        }
        super.done();
    }

    protected void updateHostBufferUtility(Message m){
        avgHops.put(m, m.getPathLength());
    }
    @Override
    public void newMessage(Message m) {
        updateHostBufferUtility(m);
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        updateHostBufferUtility(m);
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        //??if the function is called after the message transferred from the fromhost to tohost???
        if(m.getTo()==to){
            updateHostBufferUtility(m);
        }
    }

    
    
}