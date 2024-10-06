package utils.metric;

import simulator.LogicalTime;

import network.Address;
import org.json.simple.JSONObject;
import utils.Config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.stream.Collectors;


public class QualityMetric {
    private static Address first = null;
    private static int violatingPairs = 0;
    private static int inCS = 0;
    private static long vioTime = 0;
    private static int worstNum = 0;

    private static boolean inViolation = false;
    private static long startTime = 0;

    private static Set<Address> firstRecOK = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    public static JSONObject getStat(long totalTime) {
        JSONObject obj = new JSONObject();
        obj.put("unsafePairs", violatingPairs);
        obj.put("vioTimeRatio", (double) ((vioTime * 1.0) / totalTime));
        obj.put("worstVioCount", worstNum);

        return obj;
    }

    public static void updateCS(boolean entering, Address me, Set<Address> recOks) {
        long time = LogicalTime.time;
        if (entering) {
            inCS++;
            if (inCS > 1) {
                if (!inViolation) {
                    // not already violating, start measuring violation time
                    startTime = time;
                    inViolation = true;
                    System.out.println("First set by " + first + " was " + firstRecOK);
                    System.out.println("violating set by " + me + " was " + recOks);
                }
            } 
            first = me;
            firstRecOK.clear();
            for (Address a : recOks) {
                firstRecOK.add(a);
            }
        } else {
            inCS--;
            if (inCS == 1) {
                if (inViolation) {
                    vioTime += (time - startTime);
                    inViolation = false;
                }
            }
            firstRecOK.clear();
        }
        worstNum = worstNum > inCS ? worstNum : inCS; // max
    }

}
