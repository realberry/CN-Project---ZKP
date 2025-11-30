package client;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ZKPClientFailure2 {
    
    private String serverHost;
    private int serverPort;
    private Graph graph;
    private Map<Integer, String> actualColouring;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public ZKPClientFailure2(String serverHost, int serverPort, Graph graph, Map<Integer, String> colouring) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.graph = graph;
        this.actualColouring = new HashMap<>(colouring);
        
        // Modify colouring to use 4 colours instead of 3
        // Find a vertex and change its colour to a 4th colour
        System.out.println("FAILURE CASE 2: Using 4 colours instead of 3");
        
        // Get the set of colours currently used
        Set<String> usedColours = new HashSet<>(actualColouring.values());
        System.out.println("   Original colours used: " + usedColours);
        
        // Define a 4th colour that's NOT in the current colouring
        String fourthColour = "YELLOW";
        if (usedColours.contains("YELLOW")) {
            fourthColour = "ORANGE";
        }
        if (usedColours.contains(fourthColour)) {
            fourthColour = "PINK";
        }
        if (usedColours.contains(fourthColour)) {
            fourthColour = "CYAN";
        }
        
        // Find a vertex and change its colour to the 4th colour
        // Pick vertex 0 for simplicity
        int vertexToChange = 0;
        String oldColour = actualColouring.get(vertexToChange);
        actualColouring.put(vertexToChange, fourthColour);
        
        System.out.println("   Changed vertex " + vertexToChange + " from " + oldColour + " to " + fourthColour);
        System.out.println("   Now using colours: " + new HashSet<>(actualColouring.values()));
    }
    
    /**
     * Connect to the server
     */
    public void connect() throws IOException {
        System.out.println("Connecting to server at " + serverHost + ":" + serverPort + "...");
        socket = new Socket(serverHost, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");
    }
    
    /**
     * Run the ZKP protocol
     */
    public void runProtocol(int numRounds) throws IOException {
        Set<String> actualColours = new HashSet<>(actualColouring.values());
        System.out.println("\nStarting Zero-Knowledge Proof Protocol (FAILURE CASE 2)");
        System.out.println("Graph: " + graph.getNumVertices() + " vertices, " + 
                           graph.getEdges().size() + " edges");
        System.out.println("Colours used: " + actualColours.size() + " " + actualColours);
        System.out.println("Rounds: " + numRounds);
        
        for (int round = 1; round <= numRounds; round++) {
            System.out.println("Round " + round + "/" + numRounds);
            
            // Step 1: COMMIT - Generate random permutation and send commitments
            Map<String, String> colourPermutation = generatePermutation();
            Map<Integer, String> permutedColouring = applyPermutation(colourPermutation);
            Map<Integer, String> nonces = generateNonces();
            List<String> commitments = createCommitments(permutedColouring, nonces);
            
            CommitMessage commitMsg = new CommitMessage(commitments, round);
            sendMessage(commitMsg);
            System.out.println("   Sent commitments (hashed colours)");
            
            // Step 2: Receive challenge from server (or final result if server detected failure)
            String response = receiveMessage();
            ProtocolMessage msg = ProtocolMessage.fromJSON(response);
            
            // If server detected failure early, it sends RESULT instead of CHALLENGE
            if (msg instanceof ResultMessage) {
                ResultMessage result = (ResultMessage) msg;
                System.out.println();
                if (result.isVerified()) {
                    System.out.println("VERIFICATION SUCCESSFUL!");
                    System.out.println("Server confirmed: Client knows valid colouring");
                } else {
                    System.out.println("Invalid Graph was detected");
                }
                System.out.println("Total rounds completed: " + result.getTotalRounds());
                return;
            }
            
            if (!(msg instanceof ChallengeMessage)) {
                throw new IOException("Expected CHALLENGE or RESULT message");
            }
            
            ChallengeMessage challenge = (ChallengeMessage) msg;
            int v1 = challenge.getVertex1();
            int v2 = challenge.getVertex2();
            System.out.println("   Challenge: Reveal colours of vertices " + v1 + " and " + v2);
            
            // Step 3: REVEAL - Send the revealed colours and nonces
            String colour1 = permutedColouring.get(v1);
            String colour2 = permutedColouring.get(v2);
            String nonce1 = nonces.get(v1);
            String nonce2 = nonces.get(v2);
            
            RevealMessage reveal = new RevealMessage(colour1, colour2, nonce1, nonce2, round);
            sendMessage(reveal);
            System.out.println("   Revealed: v" + v1 + "=" + colour1 + ", v" + v2 + "=" + colour2);
            System.out.println();
            
            // Small delay for dramatic effect
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
                System.out.println("Invalid Graph was detected");
            }
            System.out.println("Total rounds completed: " + result.getTotalRounds());
        }
    }
    
    /**
     * Generate a random colour permutation (now includes 4 colours)
     * This ensures server can't learn the actual colouring
     */
    private Map<String, String> generatePermutation() {
        Set<String> colours = new HashSet<>(actualColouring.values());
        return CryptoUtils.generateColourPermutation(colours);
    }
    
    /**
     * Apply permutation to the actual colouring
     */
    private Map<Integer, String> applyPermutation(Map<String, String> permutation) {
        Map<Integer, String> permutedColouring = new HashMap<>();
        for (Map.Entry<Integer, String> entry : actualColouring.entrySet()) {
            String originalColour = entry.getValue();
            String permutedColour = permutation.get(originalColour);
            permutedColouring.put(entry.getKey(), permutedColour);
        }
        return permutedColouring;
    }
    
    /**
     * Generate random nonces for all vertices
     */
    private Map<Integer, String> generateNonces() {
        Map<Integer, String> nonces = new HashMap<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            nonces.put(i, CryptoUtils.generateNonce());
        }
        return nonces;
    }
    
    /**
     * Create commitments (hashes) for all vertices
     */
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
    
    /**
     * Send a message to the server
     */
    private void sendMessage(ProtocolMessage message) {
        out.println(message.toJSON());
    }
    
    /**
     * Receive a message from the server
     */
    private String receiveMessage() throws IOException {
        return in.readLine();
    }
    
    /**
     * Close the connection
     */
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
    
    /**
     * Main method - Demo client with failure case 2
     */
    public static void main(String[] args) {
        try {
            // Create sample graph
            Graph graph = Graph.createSampleGraph();
            Map<Integer, String> colouring = Graph.getSampleColouring();
            
            System.out.println("Zero-Knowledge Proof - FAILURE CASE 2");
            System.out.println("Testing: Using 4 colours instead of 3");
            System.out.println(graph);
            
            // Create client with failure case 2
            ZKPClientFailure2 client = new ZKPClientFailure2("localhost", 8888, graph, colouring);
            
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
