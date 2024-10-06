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
    private final Set<Address> pendingOKs = ConcurrentHashMap.newKeySet();
    private final Set<Long> req_times = ConcurrentHashMap.newKeySet();

    // measurement variables
    public Integer max_rec_OK = 0;

    public Server(Address id) {
        this.id = id;
        this.membership = new Membership(id);
    }


    public void processEvent(Event event) {
        Logging.log(Level.FINER, id, "Processing event: " + event.toString());
        if (event.getType() == EventType.RECEIVE_MSG) {
            processMsg(((ReceiveMsgEvent) event).getMsg());
        } else if (event.getType() == EventType.INITATE_REQ){
            initiateRequest();
        } else if (event.getType() == EventType.EXIT_CRIT) {
             // EXIT
            //  System.out.println("Exiting critical section at node " + id);
            //System.out.println("beep boop")
            AlgorithmMetric.setFirstExitTime(LogicalTime.time);
            MessagePayload release = new ReleasePayload(currSeqNum,id);
            Message rel_msg = new Message(id,id,release);
            QualityMetric.updateCount(--LogicalTime.CSct, LogicalTime.time);
            myState = AlgoState.NONE;
            Network.multicast(rel_msg, membership.getAllNodes(true));
             OkPayload ok_p = new OkPayload();
             for (RequestPayload waiting : deferred) {
                 Message ok_message = new Message(id,waiting.getId(),ok_p);
                 Network.unicast(ok_message);
             }
             deferred.clear();

             if (queuedReqs>0) {
                queuedReqs--;
                initiateRequest();
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

            maxSeqSeen = Math.max(maxSeqSeen,req.getSeqNum());

            if (myState == AlgoState.HELD || (myState == AlgoState.WAIT && req.isGreaterThan(currRequest))) {
                // defer incoming request
                deferred.add(req);
            }
            else {
                OkPayload ok_p = new OkPayload();
                Message ok_message = new Message(id,req.getId(),ok_p);
                Network.unicast(ok_message);
            }

        } else if (payload.getType() == MessageType.OK) {
            MessagePayload req = new RequestPayload(currSeqNum,id,AlgorithmPath.FAST);
            pendingOKs.remove(msg.getSrc());
            // System.out.println("Now only waiting for " + pendingOKs.size() + " OKs");
            
            // includes running critical section and sending out releases
            // System.out.println("Size of pendingOks is "+String.valueOf(pendingOKs.size()));
            if (pendingOKs.isEmpty() && myState == AlgoState.WAIT) {
                // execute the critical section
                executeCriticalSection();
            }
        } else if (payload.getType() == MessageType.RELEASE) {
            // process release;
            // recentlyOKed.remove(((ReleasePayload)payload).getReleaser());
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

    }

    public void updateMembership() {
        membership.update();
    }

    public void loadRequest(long time) {
        InitiateRequestEvent req = new InitiateRequestEvent(time, id);
        EventService.addEvent(req);
    }

    private void executeCriticalSection() {
        // System.out.println("Entering critical section at node " + id);
        AlgorithmMetric.addWaitTime(LogicalTime.time - initTime);
        if (Config.batched) {
            for (Long time : req_times) {
                AlgorithmMetric.addWaitTime(LogicalTime.time - time);
            }
        }
        AlgorithmMetric.setSecondEnterTime(LogicalTime.time);
        myState = AlgoState.HELD;
        QualityMetric.updateCount(++LogicalTime.CSct, LogicalTime.time);
        ExitCritEvent exit = new ExitCritEvent(LogicalTime.time + Config.critDuration, id);
        EventService.addEvent(exit);
    }
}
