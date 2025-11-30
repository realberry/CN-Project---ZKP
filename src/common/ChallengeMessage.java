package common;

import org.json.JSONObject;

public class ChallengeMessage extends ProtocolMessage {
    private int vertex1;
    private int vertex2;
    private int round;
    
    public ChallengeMessage(int vertex1, int vertex2, int round) {
        super(MessageType.CHALLENGE);
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.round = round;
    }
    
    public int getVertex1() {
        return vertex1;
    }
    
    public int getVertex2() {
        return vertex2;
    }
    
    public int getRound() {
        return round;
    }
    
    @Override
    public String toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("type", type.name());
        obj.put("round", round);
        obj.put("vertex1", vertex1);
        obj.put("vertex2", vertex2);
        return obj.toString();
    }
    
    public static ChallengeMessage fromJSONObject(JSONObject obj) {
        int round = obj.getInt("round");
        int v1 = obj.getInt("vertex1");
        int v2 = obj.getInt("vertex2");
        return new ChallengeMessage(v1, v2, round);
    }
}
