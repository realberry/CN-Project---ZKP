package common;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class CommitMessage extends ProtocolMessage {
    private List<String> commitments;  // Hashed colors for each vertex
    private int round;
    
    public CommitMessage(List<String> commitments, int round) {
        super(MessageType.COMMIT);
        this.commitments = commitments;
        this.round = round;
    }
    
    public List<String> getCommitments() {
        return commitments;
    }
    
    public int getRound() {
        return round;
    }
    
    @Override
    public String toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("type", type.name());
        obj.put("round", round);
        obj.put("commitments", new JSONArray(commitments));
        return obj.toString();
    }
    
    public static CommitMessage fromJSONObject(JSONObject obj) {
        int round = obj.getInt("round");
        JSONArray commitsArray = obj.getJSONArray("commitments");
        List<String> commitments = new ArrayList<>();
        for (int i = 0; i < commitsArray.length(); i++) {
            commitments.add(commitsArray.getString(i));
        }
        return new CommitMessage(commitments, round);
    }
}
