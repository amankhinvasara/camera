package utils.metric;

import network.Address;
import network.message.Message;

import org.json.simple.JSONObject;
import utils.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkMetric {
    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> e2eMsgs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> h2hMsgs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> e2eMsgSizes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> h2hMsgSizes = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> RAe2eSizeAdjustments = new ConcurrentHashMap<>();
    

    public static void initialize(List<Address> addresses) {
        for (Address ip: addresses) {
            e2eMsgs.put(ip, new HashMap<>());
            h2hMsgs.put(ip, new HashMap<>());
            e2eMsgSizes.put(ip, new HashMap<>());
            h2hMsgSizes.put(ip, new HashMap<>());
            RAe2eSizeAdjustments.put(ip, new HashMap<>());
        }
    }


    public static void e2eRecord(Address src, Address dst, int size) {
        e2eMsgs.get(src).put(dst, e2eMsgs.get(src).getOrDefault(dst, 0) + 1);
        e2eMsgSizes.get(src).put(dst, e2eMsgSizes.get(src).getOrDefault(dst, 0) + size);
    }


    public static void h2hRecord(Address src, Address dst, int size) {
        h2hMsgs.get(src).put(dst, h2hMsgs.get(src).getOrDefault(dst, 0) + 1);
        h2hMsgSizes.get(src).put(dst, h2hMsgSizes.get(src).getOrDefault(dst, 0) + size);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        Integer e2eMsgTotal = 0;
        Integer h2hMsgTotal = 0;
        Integer e2eMsgSizeTotal = 0;
        Integer h2hMsgSizeTotal = 0;
        HashMap<Address, Integer> e2eMsgsTotal = new HashMap<>();
        HashMap<Address, Integer> h2hMsgsTotal = new HashMap<>();
        HashMap<Address, Integer> e2eMsgSizesTotal = new HashMap<>();
        HashMap<Address, Integer> h2hMsgSizesTotal = new HashMap<>();

        Integer RAe2eMsgSizeTotal = 0;
        HashMap<Address, Integer> RAe2eSizeAdjustmentsTotal = new HashMap<>();


        for (Address src: e2eMsgs.keySet()) {
            Integer curr = 0;
            Integer currSize = 0;
            Integer RAcurrSize = 0;
            for (Address dst: e2eMsgs.get(src).keySet()) {
                e2eMsgTotal += e2eMsgs.get(src).get(dst);
                curr += e2eMsgs.get(src).get(dst);
                e2eMsgSizeTotal += e2eMsgSizes.get(src).get(dst);
                currSize += e2eMsgSizes.get(src).get(dst);
                try {
                    RAe2eMsgSizeTotal += RAe2eSizeAdjustments.get(src).get(dst);
                    RAcurrSize += RAe2eSizeAdjustments.get(src).get(dst);
                } catch (NullPointerException n) {
                    ;
                }
            }
            e2eMsgsTotal.put(src, curr);
            e2eMsgSizesTotal.put(src, currSize);
            RAe2eSizeAdjustmentsTotal.put(src,RAcurrSize);
        }
        for (Address src: h2hMsgs.keySet()) {
            Integer curr = 0;
            Integer currSize = 0;
            for (Address dst: h2hMsgs.get(src).keySet()) {
                h2hMsgTotal += h2hMsgs.get(src).get(dst);
                curr += h2hMsgs.get(src).get(dst);
                h2hMsgSizeTotal += h2hMsgSizes.get(src).get(dst);
                currSize += h2hMsgSizes.get(src).get(dst);
            }
            h2hMsgsTotal.put(src, curr);
            h2hMsgSizesTotal.put(src, currSize);
        }

        JSONObject obj = new JSONObject();

        obj.put("e2eMsgTotal", e2eMsgTotal);
        obj.put("h2hMsgTotal", h2hMsgTotal);
        obj.put("e2eMsgSizeTotal", e2eMsgSizeTotal);
        obj.put("h2hMsgSizeTotal", h2hMsgSizeTotal);
        obj.put("RAe2eMsgSizeTotal", e2eMsgSizeTotal + RAe2eMsgSizeTotal);

        if (Config.verbose > 0) {
            JSONObject e2eMsgsTotalObj = new JSONObject();
            JSONObject h2hMsgsTotalObj = new JSONObject();
            JSONObject e2eMsgSizesTotalObj = new JSONObject();
            JSONObject h2hMsgSizesTotalObj = new JSONObject();

            for (Address src: e2eMsgsTotal.keySet()) {
                e2eMsgsTotalObj.put(src.toString(), e2eMsgsTotal.get(src));
                e2eMsgSizesTotalObj.put(src.toString(), e2eMsgSizesTotal.get(src));
            }
            for (Address src: h2hMsgsTotal.keySet()) {
                h2hMsgsTotalObj.put(src.toString(), h2hMsgsTotal.get(src));
                h2hMsgSizesTotalObj.put(src.toString(), h2hMsgSizesTotal.get(src));
            }

            obj.put("e2eMsgsTotal", e2eMsgsTotalObj);
            obj.put("h2hMsgsTotal", h2hMsgsTotalObj);
            obj.put("e2eMsgSizesTotal", e2eMsgSizesTotalObj);
            obj.put("h2hMsgSizesTotal", h2hMsgSizesTotalObj);
        }

        if (Config.verbose > 1) {
            JSONObject e2eMsgsTotalObj = new JSONObject();
            JSONObject h2hMsgsTotalObj = new JSONObject();
            JSONObject e2eMsgSizesTotalObj = new JSONObject();
            JSONObject h2hMsgSizesTotalObj = new JSONObject();
            for (Address src: e2eMsgs.keySet()) {
                JSONObject e2eMsgsObj = new JSONObject();
                JSONObject e2eMsgSizesObj = new JSONObject();
                for (Address dst : e2eMsgs.get(src).keySet()) {
                    e2eMsgsObj.put(dst.toString(), e2eMsgs.get(src).get(dst));
                    e2eMsgSizesObj.put(dst.toString(), e2eMsgSizes.get(src).get(dst));
                }
                e2eMsgsTotalObj.put(src.toString(), e2eMsgsObj);
                e2eMsgSizesTotalObj.put(src.toString(), e2eMsgSizesObj);
            }
            for (Address src: h2hMsgs.keySet()) {
                JSONObject h2hMsgsObj = new JSONObject();
                JSONObject h2hMsgSizesObj = new JSONObject();
                for (Address dst : h2hMsgs.get(src).keySet()) {
                    h2hMsgsObj.put(dst.toString(), h2hMsgs.get(src).get(dst));
                    h2hMsgSizesObj.put(dst.toString(), h2hMsgSizes.get(src).get(dst));
                }
                h2hMsgsTotalObj.put(src.toString(), h2hMsgsObj);
                h2hMsgSizesTotalObj.put(src.toString(), h2hMsgSizesObj);
            }

            obj.put("e2eMsgs", e2eMsgsTotalObj);
            obj.put("h2hMsgs", h2hMsgsTotalObj);
            obj.put("e2eMsgSizes", e2eMsgSizesTotalObj);
            obj.put("h2hMsgSizes", h2hMsgSizesTotalObj);
        }

        return obj;
    }
}
