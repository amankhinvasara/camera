package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import simulator.LogicalTime;
import utils.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class AlgorithmMetric {
    // private static long electionStartTime, electionEndTime;
    private static long firstExitTime, secondEnterTime;
    // private static List<Long> waitTimes;
    private static List<Long> waitTimes = new ArrayList<Long>();
    private static List<Long> arrivals = new ArrayList<Long>();
    private static List<Integer> max_rec_OK_sizes = new ArrayList<Integer>();
    private static boolean firstExitSet = false;
    private static boolean secondEnterSet = false;
    private static int slows = 0;
    public static int counting_waits = 0;
    // private static List<Address> excludedSuspects;
    // private static final ConcurrentHashMap<Address, Integer> trueSuspects = new ConcurrentHashMap<>();
    // private static final ConcurrentHashMap<Address, Address> correctLeaders = new ConcurrentHashMap<>();
    // private static int leaderChanges = 0;
    // private static Address leaderIp;

    public static void setFirstExitTime(long time) {
        if (!firstExitSet) {
            firstExitTime = time;
            firstExitSet = true;
        }
    }

    public static void setSecondEnterTime(long time) {
        if (!secondEnterSet && firstExitSet) {
            secondEnterTime = time;
            secondEnterSet = true;
        }
    }

    public static void reportMaxRecOK(Integer i) {
        max_rec_OK_sizes.add(i);
    }

    public static void incrementSlows() {
        slows++;
    }    

    public static void addArrivalTime(long time) {
        // System.out.println("Added wait duration "+String.valueOf(++counting_waits));
        arrivals.add(time);
    }


    public static void addWaitTime(long duration) {
        // System.out.println("Added wait duration "+String.valueOf(++counting_waits));
        waitTimes.add(duration);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        JSONObject obj = new JSONObject();

        obj.put("clientDelay",secondEnterTime-firstExitTime);
        // obj.put("syncDelay",waitTimes.get(0));

        // System.out.println("Length of waitTimes is "+ String.valueOf(waitTimes.size()));
        
        obj.put("waitTimes",waitTimes);
        obj.put("slows",slows);
        obj.put("arrivals",arrivals);
        obj.put("max_rec_OK_sizes",max_rec_OK_sizes);
        return obj;
    }
}
