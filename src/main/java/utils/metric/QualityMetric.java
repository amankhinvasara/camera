package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import utils.Config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QualityMetric {
    private static Address leader = null;
    private static int violatingPairs = 0;
    private static long vioTime = 0;
    private static int worstNum = 0;

    private static boolean inViolation = false;
    private static long startTime = 0;

    public static void setLeader(Address leader) {
        QualityMetric.leader = leader;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat(long totalTime) {
        // ConcurrentHashMap<Address, Integer> trueSuspects = new ConcurrentHashMap<Address,Integer>();
        // // ConcurrentHashMap<Address, Integer> trueSuspects = AlgorithmMetric.getTrueSuspects();
        // List<Address> suspectCounts = trueSuspects.entrySet().stream()
        //         .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(Map.Entry::getKey).collect(Collectors.toList());

        JSONObject obj = new JSONObject();
        obj.put("unsafePairs",violatingPairs);
        obj.put("vioTimeRatio",(double)((vioTime*1.0)/totalTime));
        obj.put("worstVioCount",worstNum);
        // obj.put("suspectRank", leader == null ? -1 :
        //         suspectCounts.contains(leader) ? suspectCounts.indexOf(leader) : Config.numServers - 1);
        // obj.put("hashRank", leader == null ? -1 : leader.getId());
        // obj.put("suspectCount", trueSuspects.getOrDefault(leader, 0));

        return obj;
    }

    public static void updateCount(int ct, long time) {
        if (ct>1) {
            violatingPairs += ((ct-1)*ct)/2;
            if (!inViolation) {
                // not already violating, start measuring violation time
                startTime = time;
                inViolation = true;
            }
        } else if (ct==1) {
            if (inViolation) {
                vioTime += (time - startTime);
                inViolation = false;
            }
        }
        worstNum = worstNum > ct ? worstNum : ct; // max
    }

}
