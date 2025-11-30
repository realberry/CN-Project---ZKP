package common;

import org.json.JSONObject;

public class ResultMessage extends ProtocolMessage {
    private boolean verified;
    private String message;
    private int totalRounds;
    
    public ResultMessage(boolean verified, String message, int totalRounds) {
        super(MessageType.RESULT);
        this.verified = verified;
        this.message = message;
        this.totalRounds = totalRounds;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getTotalRounds() {
        return totalRounds;
    }
    
    @Override
    public String toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("type", type.name());
        obj.put("verified", verified);
        obj.put("message", message);
        obj.put("totalRounds", totalRounds);
        return obj.toString();
    }
    
    public static ResultMessage fromJSONObject(JSONObject obj) {
        boolean verified = obj.getBoolean("verified");
        String message = obj.getString("message");
        int totalRounds = obj.getInt("totalRounds");
        return new ResultMessage(verified, message, totalRounds);
    }
}
