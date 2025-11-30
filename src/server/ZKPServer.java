package server;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ZKPServer {
    
    private int port;
    private Graph graph;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    
    // Store commitments for current round
    private List<String> currentCommitments;
    
    // Track revealed colours across all rounds
    private Set<String> allRevealedColours;
    
    // Track failure type
    private String failureType;
    
    public ZKPServer(int port, Graph graph) {
        this.port = port;
        this.graph = graph;
        this.allRevealedColours = new HashSet<>();
        this.failureType = null;
    }
    
    // Start the server and listen for connections
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("ZKP Server started on port " + port);
        System.out.println("Graph: " + graph.getNumVertices() + " vertices, " + 
                           graph.getEdges().size() + " edges");
        System.out.println("Waiting for client connection...\n");
        
        clientSocket = serverSocket.accept();
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        
        System.out.println("Client connected from: " + clientSocket.getInetAddress());
    }

    // Run the verification protocol for specified number of rounds
    public void runProtocol(int numRounds) throws IOException {
        System.out.println("\nStarting Zero-Knowledge Verification Protocol");
        System.out.println("Rounds to execute: " + numRounds);
        
        boolean allRoundsValid = true;
        int completedRounds = 0;
        
        for (int round = 1; round <= numRounds; round++) {
            System.out.println("Round " + round + "/" + numRounds);
            
            try {
                // Step 1: Receive commitments from client
                String commitJson = receiveMessage();
                ProtocolMessage msg = ProtocolMessage.fromJSON(commitJson);
                
                if (!(msg instanceof CommitMessage)) {
                    throw new IOException("Expected COMMIT message");
                }
                
                CommitMessage commit = (CommitMessage) msg;
                currentCommitments = commit.getCommitments();
                System.out.println("   Received commitments (" + currentCommitments.size() + " vertices)");
                
                // Step 2: Select random edge and challenge client
                int[] edge = selectRandomEdge();
                int v1 = edge[0];
                int v2 = edge[1];
                
                ChallengeMessage challenge = new ChallengeMessage(v1, v2, round);
                sendMessage(challenge);
                System.out.println("   Challenge: Reveal edge (" + v1 + ", " + v2 + ")");
                
                // Step 3: Receive and verify the revealed colours
                String revealJson = receiveMessage();
                ProtocolMessage revealMsg = ProtocolMessage.fromJSON(revealJson);
                
                if (!(revealMsg instanceof RevealMessage)) {
                    throw new IOException("Expected REVEAL message");
                }
                
                RevealMessage reveal = (RevealMessage) revealMsg;
                
                // Track revealed colours
                allRevealedColours.add(reveal.getColour1());
                allRevealedColours.add(reveal.getColour2());
                
                // Check immediately if more than 3 colours detected
                if (allRevealedColours.size() > 3) {
                    System.out.println("\nDETECTED: More than 3 colours used!");
                    System.out.println("   Colours revealed: " + allRevealedColours);
                    System.out.println("   Total unique colours: " + allRevealedColours.size());
                    failureType = "FAILURE_CASE_2_TOO_MANY_COLORS";
                    allRoundsValid = false;
                    completedRounds = round;
                    
                    // Send result immediately and break
                    sendFailureResult(completedRounds, "FAILURE CASE 2: Used " + allRevealedColours.size() + " colours instead of 3!");
                    displayFinalResults(allRoundsValid, completedRounds, numRounds);
                    return;
                }
                
                // Verify this round
                boolean roundValid = verifyRound(reveal, v1, v2);
                
                if (roundValid) {
                    System.out.println("   Round " + round + " PASSED");
                    completedRounds++;
                } else {
                    System.out.println("   Round " + round + " FAILED");
                    allRoundsValid = false;
                    completedRounds = round;
                    
                    // Send result immediately and break
                    sendFailureResult(completedRounds, "FAILURE CASE 1: Adjacent vertices have same colour!");
                    displayFinalResults(allRoundsValid, completedRounds, numRounds);
                    return;
                }
                
                System.out.println();
                
                // Small delay
                Thread.sleep(100);
                
            } catch (Exception e) {
                System.err.println("   Error in round " + round + ": " + e.getMessage());
                allRoundsValid = false;
                completedRounds = round;
                sendFailureResult(completedRounds, "Verification failed. Invalid colouring or cheating detected.");
                displayFinalResults(false, completedRounds, numRounds);
                return;
            }
        }
        
        // All rounds passed successfully
        ResultMessage result = new ResultMessage(true, 
            "Verification successful! Client knows valid 3-colouring.", 
            completedRounds);
        sendMessage(result);
        displayFinalResults(true, completedRounds, numRounds);
    }
    
    /**
     * Verify a single round of the protocol
     */
    private boolean verifyRound(RevealMessage reveal, int v1, int v2) {
        String colour1 = reveal.getColour1();
        String colour2 = reveal.getColour2();
        String nonce1 = reveal.getNonce1();
        String nonce2 = reveal.getNonce2();
        
        System.out.println("   Verifying revealed colours...");
        System.out.println("      v" + v1 + " = " + colour1);
        System.out.println("      v" + v2 + " = " + colour2);
        
        // Check 1: Verify commitments match
        String commitment1 = currentCommitments.get(v1);
        String commitment2 = currentCommitments.get(v2);
        
        boolean commit1Valid = CryptoUtils.verifyCommitment(commitment1, colour1, nonce1);
        boolean commit2Valid = CryptoUtils.verifyCommitment(commitment2, colour2, nonce2);
        
        if (!commit1Valid || !commit2Valid) {
            System.out.println("      Commitment verification failed!");
            return false;
        }
        
        System.out.println("      Commitments verified");
        
        // Check 2: Verify colours are different (adjacent vertices must have different colours)
        if (colour1.equals(colour2)) {
            System.out.println("      Adjacent vertices have same colour!");
            failureType = "FAILURE_CASE_1_SAME_COLOR";
            return false;
        }
        
        System.out.println("      Colours are different");
        
        return true;
    }
    
    // Select a random edge from the graph
    private int[] selectRandomEdge() {
        List<int[]> edges = graph.getEdges();
        Random random = new Random();
        return edges.get(random.nextInt(edges.size()));
    }
    
    // Send a message to the client
    private void sendMessage(ProtocolMessage message) {
        out.println(message.toJSON());
    }
    
    // Receive a message from the client
    private String receiveMessage() throws IOException {
        return in.readLine();
    }
    
    // Helper method to send failure result
    private void sendFailureResult(int completedRounds, String failureMsg) {
        ResultMessage result = new ResultMessage(false, failureMsg, completedRounds);
        sendMessage(result);
        out.flush(); // Ensure message is sent before we return
        try {
            Thread.sleep(200); // Give client time to receive the message
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Helper method to display final results
    private void displayFinalResults(boolean allRoundsValid, int completedRounds, int numRounds) {
        System.out.println();
        if (allRoundsValid) {
            System.out.println("VERIFICATION COMPLETE - SUCCESS!");
            System.out.println("Client proved knowledge of valid 3-colouring");
            System.out.println("Server learned NOTHING about actual colours");
            System.out.println("Colours observed: " + allRevealedColours.size() + " unique colours (permuted)");
        } else {
            System.out.println("VERIFICATION FAILED");
            if ("FAILURE_CASE_1_SAME_COLOR".equals(failureType)) {
                System.out.println("FAILURE CASE 1: Adjacent vertices had same colour");
            } else if ("FAILURE_CASE_2_TOO_MANY_COLORS".equals(failureType)) {
                System.out.println("FAILURE CASE 2: Used " + allRevealedColours.size() + " colours (expected 3)");
                System.out.println("   Revealed colours: " + allRevealedColours);
            } else {
                System.out.println("Client failed to prove valid colouring");
            }
        }
        System.out.println("Rounds completed: " + completedRounds + "/" + numRounds);
    }
    
    // Stop the server and close connections
    public void stop() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("\nServer stopped");
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            // Create the same graph structure as the client
            Graph graph = Graph.createSampleGraph();
            
            System.out.println("Zero-Knowledge Proof - Graph Colouring Server");
            System.out.println(graph);
            
            // Create and start server
            ZKPServer server = new ZKPServer(8888, graph);
            server.start();
            
            // Run protocol for 100 rounds
            server.runProtocol(100);
            
            // Stop server
            server.stop();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
