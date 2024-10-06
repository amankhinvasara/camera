package simulator;

import enums.*;
import network.Address;
import network.Network;
import network.message.Message;
import network.message.payload.MessagePayload;
import network.message.payload.election.*;
import simulator.event.*;
import utils.*;
import utils.metric.AlgorithmMetric;
import utils.metric.NetworkMetric;
import utils.metric.QualityMetric;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Server {
    private final Address id;
    private final Membership membership;

    // algorithm related variables
    private int maxSeqSeen = 0;
    private int currSeqNum = 0;
    private AlgoState myState = AlgoState.NONE;
    private long initTime = 0;
    private Integer queuedReqs = 0;
    private RequestPayload currRequest;
    private final Set<RequestPayload> deferred = ConcurrentHashMap.newKeySet();
    private final Set<Address> recentlyOKed = ConcurrentHashMap.newKeySet();
    private final Set<Address> pendingOKs = ConcurrentHashMap.newKeySet();
    private final Set<Address> recOKs = ConcurrentHashMap.newKeySet();
    private final Set<Long> req_times = ConcurrentHashMap.newKeySet();

    // measurement variables
    public Integer max_rec_OK = 0;

    public Server(Address id) {
        this.id = id;
        this.membership = new Membership(id);
    }


    public void processEvent(Event event) {
        Logging.log(Level.FINER, id, "Processing event: " + event.toString());
        max_rec_OK = Math.max(max_rec_OK,recentlyOKed.size());
        if (event.getType() == EventType.RECEIVE_MSG) {
            processMsg(((ReceiveMsgEvent) event).getMsg());
        } else if (event.getType() == EventType.INITATE_REQ){
            initiateRequest();
        } else if (event.getType() == EventType.EXIT_CRIT) {
             // EXIT
            //  System.out.println(LogicalTime.time + ": Exiting critical section for request " + currSeqNum + " at node " + id );
             AlgorithmMetric.setFirstExitTime(LogicalTime.time);
             MessagePayload release = new ReleasePayload(currSeqNum,id);
             Message rel_msg = new Message(id,id,release);
             QualityMetric.updateCS(false, id, null);
             recOKs.clear();
             myState = AlgoState.NONE;
             Network.multicast(rel_msg, membership.getAllNodes(true));

             // send OK to deferred folks
             OkPayload ok_p = new OkPayload(recentlyOKed);
             for (RequestPayload waiting : deferred) {
                Message ok_message = new Message(id,waiting.getId(),ok_p);
                Network.unicast(ok_message);
                // System.out.println(LogicalTime.time + ": Just sent def OK for request " + waiting.toString() + " from server " + id);
             }
             deferred.clear();

             if (queuedReqs>0) {
                queuedReqs--;
                loadRequest(LogicalTime.time + 2);
            }
        }
        else if (event.getType() == EventType.ROUTE_MSG) {
            Network.unicast(((RouteMsgEvent) event).getMsg());

        } else if (event.getType() == EventType.RESEND) {
            Message msg = ((ResendEvent) event).getMsg();
            msg.resetCurr();

            Logging.log(Level.FINE, id, "Resending msg (" + msg + ")");
            Network.unicast(msg);
        } else {
            throw new RuntimeException("Event type " + event.getType() + " not found!!!");
        }
    }

    private void processMsg(Message msg) {
        Logging.log(Level.FINE, id, "Receiving message " + msg);
        MessagePayload payload = msg.getPayload();
        if (payload.getType() == MessageType.REQUEST) {
            // process request;
            RequestPayload req = (RequestPayload) payload;
            if (!membership.getAllNodes(false).contains(req.getId())) {
                // we don't know about the requesting node
                if (myState == AlgoState.WAIT) {
                    Message re_msg = new Message(id, req.getId(), currRequest);
                    pendingOKs.add(req.getId());
                    Network.unicast(re_msg);
                }
            }

            // System.out.print(LogicalTime.time + ": Processing req " + req.toString() + " at server " + id + " with original maxSeen " + maxSeqSeen);
            maxSeqSeen = Math.max(maxSeqSeen,req.getSeqNum());
            // System.out.println(" and new maxSeqSeen " + maxSeqSeen);

            if (myState == AlgoState.HELD || (myState == AlgoState.WAIT && req.isGreaterThan(currRequest))) {
                // defer incoming request
                // System.out.println(LogicalTime.time + ": Defering request " + req.getSeqNum() + " from server " + req.getId() + " at node " + id);
                deferred.add(req);
            }
            else {
                OkPayload ok_p = new OkPayload(recentlyOKed);
                recentlyOKed.add(req.getId());
                Message ok_message = new Message(id,req.getId(),ok_p);
                Network.unicast(ok_message);

                // measure base RA performance separately
                NetworkMetric.RAe2eAdjust(id, req.getId(), recentlyOKed);

                // System.out.println(LogicalTime.time + ": Just sent reg OK to " + req.getId() + " from server " + id + " in state " + myState + 
                //  " and with request tuples " + req.toString() + " and " + (myState==AlgoState.WAIT ? currRequest.toString() : "not waiting"));
            }

        } else if (payload.getType() == MessageType.OK) {
            OkPayload ok = (OkPayload) payload;
            MessagePayload req = new RequestPayload(currSeqNum,id,AlgorithmPath.FAST);
            // System.out.println("Size of recentlyOKed upon receiving the OK message is " + ((OkPayload) payload).getRecentlyOKed().size());
            for (Address node : ok.getRecentlyOKed()) {
                if (!membership.getAllNodes(false).contains(node)) {
                    // we don't know ab some node that was recentlyOKed
                    pendingOKs.add(node);
                    membership.addNodeToAll(node);
                    Message req_msg = new Message(id, node, req);
                    Network.unicast(req_msg);
                }
            }
            // System.out.println(LogicalTime.time + ": Just received OK from " + msg.getSrc().getId() + " at server " + id) ;
            pendingOKs.remove(msg.getSrc());
            recOKs.add(msg.getSrc());
            // System.out.println("Now only waiting for " + pendingOKs.size() + " OKs");
            
            // includes running critical section and sending out releases
            // System.out.println("Size of pendingOks is "+String.valueOf(pendingOKs.size()));
            if (pendingOKs.isEmpty() && myState == AlgoState.WAIT) {
                // execute the critical section
                // System.out.println(LogicalTime.time + ": Entering critical section for request " + currSeqNum + " at node " + id);
                executeCriticalSection();
            }
        } else if (payload.getType() == MessageType.RELEASE) {
            // process release;
            recentlyOKed.remove(((ReleasePayload)payload).getReleaser());
        }
        else {
            throw new RuntimeException("Message payload type " + payload.getType() + " not found!!!");
        }
    }

    public void initiateRequest() {

        // local serialization
        if (myState==AlgoState.WAIT || myState==AlgoState.HELD) {
            if (Config.batched) {
                if (myState==AlgoState.WAIT){
                    req_times.add(LogicalTime.time);
                    AlgorithmMetric.addArrivalTime(LogicalTime.time);
                } else if (myState==AlgoState.HELD) {
                     AlgorithmMetric.addWaitTime(0); // we already in the CS, so batch for free
                }
            } else {
                queuedReqs++;

            }
            return;
        } 

        AlgorithmMetric.addArrivalTime(LogicalTime.time);

        myState = AlgoState.WAIT;
        initTime = LogicalTime.time;
        maxSeqSeen++;
        currSeqNum = maxSeqSeen;
        pendingOKs.clear();
        recOKs.clear();
        for (Address i : membership.getAllNodes(true)) {
            pendingOKs.add(i);
        }

        if (pendingOKs.size() <= Config.numServers/2) {
            AlgorithmMetric.incrementSlows();
        }
        currRequest = new RequestPayload(currSeqNum,id,AlgorithmPath.FAST);

        for (Address ip: pendingOKs) {
            Message msg = new Message(id, ip, currRequest);
            Network.unicast(msg);
        }
        // System.out.println(LogicalTime.time + ": initiating request " + currSeqNum + " at server " + id);
    }

    public void updateMembership() {
        membership.update();
    }

    public void loadRequest(long time) {
        InitiateRequestEvent req = new InitiateRequestEvent(time, id);
        EventService.addEvent(req);
    }

    private void executeCriticalSection() {
        AlgorithmMetric.addWaitTime(LogicalTime.time - initTime);
        if (Config.batched) {
            for (Long time : req_times) {
                AlgorithmMetric.addWaitTime(LogicalTime.time - time);
            }
        }
        AlgorithmMetric.setSecondEnterTime(LogicalTime.time);
        myState = AlgoState.HELD;
        QualityMetric.updateCS(true, id, recOKs);
        ExitCritEvent exit = new ExitCritEvent(LogicalTime.time + Config.critDuration, id);
        EventService.addEvent(exit);
    }
}
