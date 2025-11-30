package common;

import org.json.JSONObject;

public class RevealMessage extends ProtocolMessage {
    private String colour1;
    private String colour2;
    private String nonce1;  // Random nonce used in hashing
    private String nonce2;
    private int round;
    
    public RevealMessage(String colour1, String colour2, String nonce1, String nonce2, int round) {
        super(MessageType.REVEAL);
        this.colour1 = colour1;
        this.colour2 = colour2;
        this.nonce1 = nonce1;
        this.nonce2 = nonce2;
        this.round = round;
    }
    
    public String getColour1() {
        return colour1;
    }
    
    public String getColour2() {
        return colour2;
    }
    
    public String getNonce1() {
        return nonce1;
    }
    
    public String getNonce2() {
        return nonce2;
    }
    
    public int getRound() {
        return round;
    }
    
    @Override
    public String toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("type", type.name());
        obj.put("round", round);
        obj.put("colour1", colour1);
        obj.put("colour2", colour2);
        obj.put("nonce1", nonce1);
        obj.put("nonce2", nonce2);
        return obj.toString();
    }
    
    public static RevealMessage fromJSONObject(JSONObject obj) {
        int round = obj.getInt("round");
        String c1 = obj.getString("colour1");
        String c2 = obj.getString("colour2");
        String n1 = obj.getString("nonce1");
        String n2 = obj.getString("nonce2");
        return new RevealMessage(c1, c2, n1, n2, round);
    }
}
