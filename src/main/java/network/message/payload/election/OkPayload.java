package network.message.payload.election;

import enums.MessageType;
import enums.AlgorithmPath;
import network.message.payload.MessagePayload;

public class OkPayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;
    private final AlgorithmPath path;

    public OkPayload() {
        super(MessageType.OK);
        this.path = AlgorithmPath.FAST;
    }

    public OkPayload(AlgorithmPath path) {
        super(MessageType.OK);
        this.path = path;
    }
    
    public AlgorithmPath getPath() {
        return path;
    }
}
