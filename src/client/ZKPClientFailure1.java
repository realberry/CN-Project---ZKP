package client;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ZKPClientFailure1 {
    private String serverHost;
    private int serverPort;
    private Graph graph;
    private Map<Integer, String> actualColouring;
    private int[] invalidEdge;  // The edge we'll make invalid
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public ZKPClientFailure1(String serverHost, int serverPort, Graph graph, Map<Integer, String> colouring) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.graph = graph;
        this.actualColouring = new HashMap<>(colouring);
        
        // Select a random edge and make both vertices the same colour
        List<int[]> edges = graph.getEdges();
        Random random = new Random();
        this.invalidEdge = edges.get(random.nextInt(edges.size()));
        
        int v1 = invalidEdge[0];
        int v2 = invalidEdge[1];
        
        System.out.println("FAILURE CASE 1: Creating invalid colouring");
        System.out.println("   Forcing edge (" + v1 + ", " + v2 + ") to have same colour");
        
        // Make both vertices the same colour
        String sameColour = actualColouring.get(v1);
        actualColouring.put(v2, sameColour);
        
        System.out.println("   Both vertices now: " + sameColour);
    }
    
    // Connect to the server
    public void connect() throws IOException {
        System.out.println("Connecting to server at " + serverHost + ":" + serverPort + "...");
        socket = new Socket(serverHost, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server!");
    }
    
    // Run the ZKP protocol
    public void runProtocol(int numRounds) throws IOException {
        System.out.println("\nStarting Zero-Knowledge Proof Protocol (FAILURE CASE 1)");
        System.out.println("Graph: " + graph.getNumVertices() + " vertices, " + 
                           graph.getEdges().size() + " edges");
        System.out.println("Invalid edge: (" + invalidEdge[0] + ", " + invalidEdge[1] + ")");
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
            
            // Step 3: Send the revealed colours and nonces
            String colour1 = permutedColouring.get(v1);
            String colour2 = permutedColouring.get(v2);
            String nonce1 = nonces.get(v1);
            String nonce2 = nonces.get(v2);
            
            RevealMessage reveal = new RevealMessage(colour1, colour2, nonce1, nonce2, round);
            sendMessage(reveal);
            
            // Check if the challenged edge was the invalid one
            if ((v1 == invalidEdge[0] && v2 == invalidEdge[1]) || 
                (v1 == invalidEdge[1] && v2 == invalidEdge[0])) {
                System.out.println("   Revealed: v" + v1 + "=" + colour1 + ", v" + v2 + "=" + colour2 + " (Invalid edge!)");
            } else {
                System.out.println("   Revealed: v" + v1 + "=" + colour1 + ", v" + v2 + "=" + colour2);
            }
            System.out.println();
            
            // Small delay for dramatic effect
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // If we complete all rounds without detection, receive final result
        String resultJson = receiveMessage();
        ProtocolMessage resultMsg = ProtocolMessage.fromJSON(resultJson);
        
        System.out.println();
        if (resultMsg instanceof ResultMessage) {
            ResultMessage result = (ResultMessage) resultMsg;
            if (result.isVerified()) {
                System.out.println("VERIFICATION SUCCESSFUL!");
                System.out.println("Server confirmed: Client knows valid colouring");
                System.out.println("(Invalid edge was not challenged in " + result.getTotalRounds() + " rounds)");
            } else {
                System.out.println("Invalid Graph was detected");
            }
            System.out.println("Total rounds completed: " + result.getTotalRounds());
        }
    }
    
    private Map<String, String> generatePermutation() {
        Set<String> colours = new HashSet<>(actualColouring.values());
        return CryptoUtils.generateColourPermutation(colours);
    }
    
    private Map<Integer, String> applyPermutation(Map<String, String> permutation) {
        Map<Integer, String> permutedColouring = new HashMap<>();
        for (Map.Entry<Integer, String> entry : actualColouring.entrySet()) {
            String originalColour = entry.getValue();
            String permutedColour = permutation.get(originalColour);
            permutedColouring.put(entry.getKey(), permutedColour);
        }
        return permutedColouring;
    }
    
    private Map<Integer, String> generateNonces() {
        Map<Integer, String> nonces = new HashMap<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            nonces.put(i, CryptoUtils.generateNonce());
        }
        return nonces;
    }
    
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
            
            System.out.println("Zero-Knowledge Proof - FAILURE CASE 1");
            System.out.println("Testing: Adjacent vertices with SAME colour");
            System.out.println(graph);
            
            // Create client with failure case 1
            ZKPClientFailure1 client = new ZKPClientFailure1("localhost", 8888, graph, colouring);
            
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
