package client;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ZKPClient {
    private String serverHost;
    private int serverPort;
    private Graph graph;
    private Map<Integer, String> actualColouring;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public ZKPClient(String serverHost, int serverPort, Graph graph, Map<Integer, String> colouring) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.graph = graph;
        this.actualColouring = colouring;
        
        // Validate the colouring before starting
        // Apply colouring temporarily for validation
        for (Map.Entry<Integer, String> entry : colouring.entrySet()) {
            graph.setColour(entry.getKey(), entry.getValue());
        }
        if (!graph.isValidColouring()) {
            throw new IllegalArgumentException("Invalid colouring provided!");
        }
    }
    
    // connect to server
    public void connect() throws IOException {
        System.out.println("Connecting to server at " + serverHost + ":" + serverPort + "...");
        socket = new Socket(serverHost, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");
    }
    
    // start the ZKP protocol
    public void runProtocol(int numRounds) throws IOException {
        System.out.println("\nStarting Zero-Knowledge Proof Protocol");
        System.out.println("Graph: " + graph.getNumVertices() + " vertices, " + 
                           graph.getEdges().size() + " edges");
        System.out.println("Rounds: " + numRounds);
        
        for (int round = 1; round <= numRounds; round++) {
            System.out.println("Round " + round + "/" + numRounds);
            
            // Step 1: Generate random permutation and send commitments
            Map<String, String> colourPermutation = generatePermutation();
            Map<Integer, String> permutedColouring = applyPermutation(colourPermutation);
            Map<Integer, String> nonces = generateNonces();
            List<String> commitments = createCommitments(permutedColouring, nonces);
            
            CommitMessage commitMsg = new CommitMessage(commitments, round);
            sendMessage(commitMsg);
            System.out.println("   Sent commitments (hashed colours)");
            
            // Step 2: Receive challenge from server
            String response = receiveMessage();
            ProtocolMessage msg = ProtocolMessage.fromJSON(response);
            
            if (!(msg instanceof ChallengeMessage)) {
                throw new IOException("Expected CHALLENGE message");
            }
            
            ChallengeMessage challenge = (ChallengeMessage) msg;
            int v1 = challenge.getVertex1();
            int v2 = challenge.getVertex2();
            System.out.println("   Challenge: Reveal colours of vertices " + v1 + " and " + v2);
            
            // Step 3: Send the revealed colours and nonces
            String colour1 = permutedColouring.get(v1);
            String colour2 = permutedColouring.get(v2);
            String nonce1 = nonces.get(v1);
            String nonce2 = nonces.get(v2);
            
            RevealMessage reveal = new RevealMessage(colour1, colour2, nonce1, nonce2, round);
            sendMessage(reveal);
            System.out.println("   Revealed: v" + v1 + "=" + colour1 + ", v" + v2 + "=" + colour2);
            System.out.println();
            
            // Small delay between rounds for readability/showcasing
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Receive final result
        String resultJson = receiveMessage();
        ProtocolMessage resultMsg = ProtocolMessage.fromJSON(resultJson);
        
        if (resultMsg instanceof ResultMessage) {
            ResultMessage result = (ResultMessage) resultMsg;
            System.out.println();
            if (result.isVerified()) {
                System.out.println("VERIFICATION SUCCESSFUL!");
                System.out.println("Server confirmed: Client knows valid colouring");
            } else {
                System.out.println("VERIFICATION FAILED!");
                System.out.println("Message: " + result.getMessage());
            }
            System.out.println("Total rounds completed: " + result.getTotalRounds());
        }
    }
    
    // Generate a random colour permutation so that verifier can't figure out the actual colouring
    private Map<String, String> generatePermutation() {
        Set<String> colours = new HashSet<>(actualColouring.values());
        return CryptoUtils.generateColourPermutation(colours);
    }
    
    // Apply permutation to the actual colouring
    private Map<Integer, String> applyPermutation(Map<String, String> permutation) {
        Map<Integer, String> permutedColouring = new HashMap<>();
        for (Map.Entry<Integer, String> entry : actualColouring.entrySet()) {
            String originalColour = entry.getValue();
            String permutedColour = permutation.get(originalColour);
            permutedColouring.put(entry.getKey(), permutedColour);
        }
        return permutedColouring;
    }
    
    // Generate random nonces for all vertices
    private Map<Integer, String> generateNonces() {
        Map<Integer, String> nonces = new HashMap<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            nonces.put(i, CryptoUtils.generateNonce());
        }
        return nonces;
    }
    
    // Create commitments for the colouring using nonces
    private List<String> createCommitments(Map<Integer, String> colouring, Map<Integer, String> nonces) {
        List<String> commitments = new ArrayList<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            String colour = colouring.get(i);
            String nonce = nonces.get(i);
            String commitment = CryptoUtils.createCommitment(colour, nonce);
            commitments.add(commitment);
        }
        return commitments;
    }
    
    private void sendMessage(ProtocolMessage message) {
        out.println(message.toJSON());
    }
    
    private String receiveMessage() throws IOException {
        return in.readLine();
    }
    
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("\nDisconnected from server");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            // Create sample graph
            Graph graph = Graph.createSampleGraph();
            Map<Integer, String> colouring = Graph.getSampleColouring();
            
            System.out.println("Zero-Knowledge Proof - Graph Colouring Client");

            System.out.println(graph);
            
            // Create client
            ZKPClient client = new ZKPClient("localhost", 8888, graph, colouring);
            
            // Connect and run protocol
            client.connect();
            client.runProtocol(100);  // 100 rounds
            
            // Close connection
            client.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
