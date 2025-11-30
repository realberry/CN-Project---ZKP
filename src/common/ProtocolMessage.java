package common;

import org.json.JSONObject;

public abstract class ProtocolMessage {
    public enum MessageType {
        COMMIT,
        CHALLENGE,
        REVEAL,
        RESULT
    }
    
    protected MessageType type;
    
    public ProtocolMessage(MessageType type) {
        this.type = type;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public abstract String toJSON();
    
    public static ProtocolMessage fromJSON(String json) {
        JSONObject obj = new JSONObject(json);
        String typeStr = obj.getString("type");
        MessageType type = MessageType.valueOf(typeStr);
        
        switch (type) {
            case COMMIT:
                return CommitMessage.fromJSONObject(obj);
            case CHALLENGE:
                return ChallengeMessage.fromJSONObject(obj);
            case REVEAL:
                return RevealMessage.fromJSONObject(obj);
            case RESULT:
                return ResultMessage.fromJSONObject(obj);
            default:
                throw new IllegalArgumentException("Unknown message type: " + typeStr);
        }
    }
}
