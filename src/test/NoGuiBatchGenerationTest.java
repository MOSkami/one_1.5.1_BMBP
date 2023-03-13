package test;

import core.*;
import gui.DTNSimGUI;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import movement.ExternalMovement;
import movement.MovementModel;
import report.Report;
import ui.DTNSimTextUI;
import ui.DTNSimUI;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class NoGuiBatchGenerationTest extends DTNSimUI {

    public static final String BATCH_MODE_FLAG = "-b";
    public static final String RANGE_DELIMETER = ":";
    public static final String RESET_METHOD_NAME = "reset";
    private static final String REPORT_PAC = "report.";
    private static List<Class<?>> resetList = new ArrayList<Class<?>>();

    private static Settings settings = null;
    public static void main(String[] args) {

        boolean batchMode = false;
        int nrofRuns[] = {0,1};
        String confFiles[];
        int firstConfIndex = 0;
        int guiIndex = 0;
        NoGuiBatchGenerationTest ngt = new NoGuiBatchGenerationTest();
        /* set US locale to parse decimals in consistent way */
        java.util.Locale.setDefault(java.util.Locale.US);

        if (args.length > 0) {
            if (args[0].equals(BATCH_MODE_FLAG)) {
                batchMode = true;
                if (args.length == 1) {
                    firstConfIndex = 1;
                }
                else {
                    nrofRuns = parseNrofRuns(args[1]);
                    firstConfIndex = 2;
                }
            }
            else { /* GUI mode */
                try { /* is there a run index for the GUI mode ? */
                    guiIndex = Integer.parseInt(args[0]);
                    firstConfIndex = 1;
                } catch (NumberFormatException e) {
                    firstConfIndex = 0;
                }
            }
            confFiles = args;
        }
        else {
            confFiles = new String[] {null};
        }

        initSettings(confFiles, firstConfIndex);

        if (batchMode) {
            long startTime = System.currentTimeMillis();
            for (int i=nrofRuns[0]; i<nrofRuns[1]; i++) {
                print("Run " + (i+1) + "/" + nrofRuns[1]);
                Settings.setRunIndex(i);
                resetForNextRun();
//                new DTNSimTextUI().start();
            }
            double duration = (System.currentTimeMillis() - startTime)/1000.0;
            print("---\nAll done in " + String.format("%.2f", duration) + "s");
        }
        else {
            Settings.setRunIndex(guiIndex);
            settings = new Settings();
            String[] bufferManagements = new String[]{
                                                        "AntPlusBufferManagement",
                                                        "AntPlus2BufferManagement",
                                                        "BMBPBufferManagement",
                                                        "FifoBufferManagement",
                                                        "HopsBufferManagement",
                                                        "RandomBufferManagement"};
//            for(String bm:bufferManagements) {
                for (int i = 5; i <= 50; i+=5) {
//                    for (int j = 3600; j <= 25200; j += 7200) {
//                        for(double k = 0.00025;)
            String bm = "AntPlus3BufferManagement";
//            for(double initialPheromone = 0.01f;initialPheromone <= 2.0f;initialPheromone+=0.01f) {
                    reset();
                    int j = 7200;
                    Double initialPheromone = 1.13;
                        if (bm.equals("TtlBufferManagement")){
                            continue;
                        }
                        if(bm.equals("AntPlus2BufferManagement") || bm.equals("AntPlusBufferManagement"))
                            settings.setSetting("Group.Ant.initialPheromone", 0.0037+"");
                        if(bm.equals("AntBufferManagement") || bm.equals("BMBPBufferManagement"))
                            settings.setSetting("Group.Ant.initialPheromone", initialPheromone+"");
//                        settings.setSetting("Report.reportDir", "reports_transmitSpeed/");
                        settings.setSetting("Report.reportDir", "reports/");
                        settings.setSetting("Group.bufferManagement", bm);
//                        settings.setSetting("btInterface.transmitSpeed", i + "k");
                        settings.setSetting("Group.bufferSize", i + "M");
                        settings.setSetting("Scenario.endTime", j + "");
                        ngt.initModel();
                        ngt.runSim();
                        print("bufferManagement:" + bm + ";bufferSize:" + i + "M,endTime:" + j);
                        print("bufferManagement:" + bm +" ;transmitSpeed:" + i +"k;initialPheromone:"
                                + initialPheromone + ",endTime:" + j);
                    }
                }
//            }
        }
//    }
    private static void resetForNextRun() {
        for (Class<?> c : resetList) {
            try {
                Method m = c.getMethod(RESET_METHOD_NAME);
                m.invoke(null);
            } catch (Exception e) {
                System.err.println("Failed to reset class " + c.getName());
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
    @Override
    protected void runSim() {
        double simTime = SimClock.getTime();
        double endTime = scen.getEndTime();

        // Startup DTN2Manager
        // XXX: Would be nice if this wasn't needed..
        DTN2Manager.setup(world);

        // 如果暂停中 等待并释放CPU
        while (simTime < endTime && !simCancelled) {
                try {
                    world.update();
                } catch (AssertionError e) {
                    // handles both assertion errors and SimErrors
                    e.printStackTrace();
                }
                simTime = SimClock.getTime();
        }

        simDone = true;
        done();
    }

    public static void reset(){
        SimScenario.reset();
        Message.reset();
        DTNHost.reset();
        MovementModel.reset();
        SimClock.reset();
        NetworkInterface.reset();
        ExternalMovement.reset();
    }

    public void initModel() {
        try {
            this.scen = SimScenario.getInstance();
            // add reports  添加最终的输出结果集合
            for (int i=1, n = settings.getInt(NROF_REPORT_S); i<=n; i++){
                String reportClass = settings.getSetting(REPORT_S + i);
                addReport((Report)settings.createObject(REPORT_PAC +
                        reportClass));
            }

            // 预热网络参数
            double warmupTime = 0;
            if (settings.contains(MM_WARMUP_S)) {
                warmupTime = settings.getDouble(MM_WARMUP_S);
                if (warmupTime > 0) {
                    SimClock c = SimClock.getInstance();
                    c.setTime(-warmupTime);
                }
            }

            // 获取当前的sim世界
            this.world = this.scen.getWorld();
            world.warmupMovementModel(warmupTime);
        }
        catch (SettingsError se) {
            System.err.println("Can't start: error in configuration file(s)");
            System.err.println(se.getMessage());
            System.exit(-1);
        }
        catch (SimError er) {
            System.err.println("Can't start: " + er.getMessage());
            System.err.println("Caught at " + er.getStackTrace()[0]);
            System.exit(-1);
        }
    }
    private static int[] parseNrofRuns(String arg) {
        int val[] = {0,1};
        try {
            if (arg.contains(RANGE_DELIMETER)) {
                val[0] = Integer.parseInt(arg.substring(0,
                        arg.indexOf(RANGE_DELIMETER))) - 1;
                val[1] = Integer.parseInt(arg.substring(arg.
                        indexOf(RANGE_DELIMETER) + 1, arg.length()));
            }
            else {
                val[0] = 0;
                val[1] = Integer.parseInt(arg);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid argument '" + arg + "' for" +
                    " number of runs");
            System.err.println("The argument must be either a single value, " +
                    "or a range of values (e.g., '2:5'). Note that this " +
                    "option has changed in version 1.3.");
            System.exit(-1);
        }

        if (val[0] < 0) {
            System.err.println("Starting run value can't be smaller than 1");
            System.exit(-1);
        }
        if (val[0] >= val[1]) {
            System.err.println("Starting run value can't be bigger than the " +
                    "last run value");
            System.exit(-1);
        }

        return val;
    }

    private static void initSettings(String[] confFiles, int firstIndex) {
        int i = firstIndex;

        if (i >= confFiles.length) {
            return;
        }

        try {
            Settings.init(confFiles[i]);
            for (i=firstIndex+1; i<confFiles.length; i++) {
                Settings.addSettings(confFiles[i]);
            }
        }
        catch (SettingsError er) {
            try {
                Integer.parseInt(confFiles[i]);
            }
            catch (NumberFormatException nfe) {
                /* was not a numeric value */
                System.err.println("Failed to load settings: " + er);
                System.err.println("Caught at " + er.getStackTrace()[0]);
                System.exit(-1);
            }
            System.err.println("Warning: using deprecated way of " +
                    "expressing run indexes. Run index should be the " +
                    "first option, or right after -b option (optionally " +
                    "as a range of start and end values).");
            System.exit(-1);
        }
    }

    private static void print(String txt) {
        System.out.println(txt);
    }
}
