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
public class BufferAvgUtilityReport extends Report implements MessageListener{
        
    public static final String Header="#the average buffer utility for each node during the whole simulation";
    
    public static final String ColumnNames="hostid bufferSize bufferUsedAvg bufferUtilityRatioAvg";
    
    protected Map<DTNHost,Double> avgUtility;
    
    protected double prevBufferUsage;
    protected double bufferUsage;

    public BufferAvgUtilityReport(){
        init();
    }
    
    @Override
    protected void init(){
        super.init();
        avgUtility=new HashMap<DTNHost,Double>();
        prevBufferUsage=0;
        bufferUsage=0;
    }
    
    @Override
    public void done(){
        if(avgUtility.isEmpty()){
            write("#There is no message transferred");
        }
        else{
            String resultMsg = Header+"\n"+ColumnNames+'\n';
            Set<Map.Entry<DTNHost,Double>>set=avgUtility.entrySet();
            double bufferUsedAvg_sum = 0;
            double bufferUtilityRatioAvg_sum = 0;
            int count = 0;
            for(Iterator<Map.Entry<DTNHost,Double>> it=set.iterator();it.hasNext();){
                Map.Entry<DTNHost,Double> entry=(Map.Entry<DTNHost,Double>)it.next();
                resultMsg+=String.format("%4d",entry.getKey().getAddress())+" "
                        +String.format("%10.2f", this.getBufferTotal(entry.getKey()))+" "
                        +format(this.getBufferUsed(entry.getKey()))+" "//在程序退出前一刻的已使用缓存
                        +format(entry.getValue())+"\n";
                bufferUsedAvg_sum += this.getBufferUsed(entry.getKey());
                bufferUtilityRatioAvg_sum += entry.getValue();
                count ++;
            }
            resultMsg+=String.format("Avg")+" "
                    +5000000.00+" "
                    +format(bufferUsedAvg_sum / count)+" "//在程序退出前一刻的已使用缓存
                    +format(bufferUtilityRatioAvg_sum / count)+"\n";
            write(resultMsg);
        }
        super.done();
    }

    protected void updateHostBufferUtility(DTNHost host){
        prevBufferUsage=avgUtility.containsKey(host)?avgUtility.get(host):0;
        bufferUsage=host.getBufferOccupancy(); 
        avgUtility.put(host, (0.5*(bufferUsage+prevBufferUsage)));
    }
    @Override
    public void newMessage(Message m) {
        updateHostBufferUtility(m.getFrom());
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if(dropped){
            updateHostBufferUtility(where);
        }
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        //??if the function is called after the message transferred from the fromhost to tohost???
        updateHostBufferUtility(from);
        updateHostBufferUtility(to);
    }
    
    protected double getBufferUsed(DTNHost host){
        return host.getRouter().getBufferSize()-host.getRouter().getFreeBufferSize();
    }
    
    protected double getBufferTotal(DTNHost host){
        return host.getRouter().getBufferSize();
    }
    
    
}