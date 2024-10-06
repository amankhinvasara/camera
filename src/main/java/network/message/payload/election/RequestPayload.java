package network.message.payload.election;

import enums.MessageType;
import enums.AlgorithmPath;
import network.Address;
import utils.AddressComparator;
import network.message.payload.MessagePayload;
// import utils.Config;

public class RequestPayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;

    private final int seqNum; // sequence number of request
    private final Address id;
    private final AlgorithmPath path;
    // private final int numSuspectCount; // number of high suspect count nodes

    public RequestPayload() {
        super(MessageType.REQUEST);
        // assert Config.algorithm != 3;
        this.seqNum = -1;
        this.path = AlgorithmPath.FAST;
        this.id = null;
    }

    public RequestPayload(int seqNum, Address id, AlgorithmPath path) {
        super(MessageType.REQUEST);
        // assert Config.algorithm == 3 || Config.algorithm == 4;
        this.seqNum = seqNum;
        this.id = id;
        this.path = path;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public Address getId() {
        return id;
    }
    
    public AlgorithmPath getPath() {
        return path;
    }

    public boolean isGreaterThan(RequestPayload other) {
        if (seqNum > other.seqNum) return true;
        if (seqNum < other.seqNum) return false;
        if (id.getId() > other.id.getId()) return true;
        return false;
    }

    public String toString() {
        return seqNum + "," + id;
    }
}
