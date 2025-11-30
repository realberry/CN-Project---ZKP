package common;

import java.util.*;

public class Graph {
    
    public static final List<String> COLOR_POOL = Arrays.asList(
        "RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "PINK", "CYAN"
    );
    
    private int numVertices;
    private Map<Integer, List<Integer>> adjacencyList;
    private Map<Integer, String> colouring;
    
    public Graph(int numVertices) {
        this.numVertices = numVertices;
        this.adjacencyList = new HashMap<>();
        this.colouring = new HashMap<>();
        
        // Initialize adjacency list
        for (int i = 0; i < numVertices; i++) {
            adjacencyList.put(i, new ArrayList<>());
        }
    }
    
    public void addEdge(int v1, int v2) {
        if (v1 < 0 || v1 >= numVertices || v2 < 0 || v2 >= numVertices) {
            throw new IllegalArgumentException("Invalid vertex index");
        }
        
        adjacencyList.get(v1).add(v2);
        adjacencyList.get(v2).add(v1);
    }
    
    public void setColour(int vertex, String colour) {
        if (vertex < 0 || vertex >= numVertices) {
            throw new IllegalArgumentException("Invalid vertex index");
        }
        colouring.put(vertex, colour);
    }
    
    public String getColour(int vertex) {
        return colouring.get(vertex);
    }
    
    public boolean isValidColouring() {
        if (colouring.size() != numVertices) {
            return false;
        }
        
        // Check that at most 3 colours are used
        Set<String> usedColours = new HashSet<>(colouring.values());
        if (usedColours.size() > 3) {
            return false;
        }
        
        for (int vertex = 0; vertex < numVertices; vertex++) {
            String vertexColour = colouring.get(vertex);
            if (vertexColour == null) {
                return false;
            }
            
            // Check all neighbors
            for (int neighbor : adjacencyList.get(vertex)) {
                String neighborColour = colouring.get(neighbor);
                if (neighborColour == null || vertexColour.equals(neighborColour)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public List<Integer> getNeighbors(int vertex) {
        return new ArrayList<>(adjacencyList.get(vertex));
    }
    
    public int getNumVertices() {
        return numVertices;
    }
    
    public List<int[]> getEdges() {
        List<int[]> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        for (int v1 = 0; v1 < numVertices; v1++) {
            for (int v2 : adjacencyList.get(v1)) {
                String edge = Math.min(v1, v2) + "-" + Math.max(v1, v2);
                if (!seen.contains(edge)) {
                    edges.add(new int[]{v1, v2});
                    seen.add(edge);
                }
            }
        }
        
        return edges;
    }
    
    public Map<Integer, String> getColouring() {
        return new HashMap<>(colouring);
    }
    
    public static Graph createSampleGraph() {
        Graph graph = new Graph(10);
        
        // Create outer pentagon (vertices 0-4)
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);
        graph.addEdge(4, 0);
        
        // Create inner pentagon (vertices 5-9)
        graph.addEdge(5, 6);
        graph.addEdge(6, 7);
        graph.addEdge(7, 8);
        graph.addEdge(8, 9);
        graph.addEdge(9, 5);
        
        // Connect outer to inner (star connections)
        graph.addEdge(0, 5);
        graph.addEdge(1, 6);
        graph.addEdge(2, 7);
        graph.addEdge(3, 8);
        graph.addEdge(4, 9);
        
        // Add some cross connections
        graph.addEdge(0, 2);
        graph.addEdge(5, 7);
        graph.addEdge(1, 3);
        graph.addEdge(6, 8);
        
        return graph;
    }
    
    public static Map<Integer, String> getSampleColouring() {
        // Select 3 random colours from the pool
        List<String> shuffled = new ArrayList<>(COLOR_POOL);
        Collections.shuffle(shuffled);
        String colour1 = shuffled.get(0);
        String colour2 = shuffled.get(1);
        String colour3 = shuffled.get(2);
        
        Map<Integer, String> colouring = new HashMap<>();
        // Outer pentagon
        colouring.put(0, colour1);
        colouring.put(1, colour2);
        colouring.put(2, colour3);
        colouring.put(3, colour1);
        colouring.put(4, colour3);
        
        // Inner pentagon
        colouring.put(5, colour2);
        colouring.put(6, colour3);
        colouring.put(7, colour1);
        colouring.put(8, colour2);
        colouring.put(9, colour1);
        
        return colouring;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph with ").append(numVertices).append(" vertices:\n");
        
        for (int v = 0; v < numVertices; v++) {
            sb.append("Vertex ").append(v);
            if (colouring.containsKey(v)) {
                sb.append(" [").append(colouring.get(v)).append("]");
            }
            sb.append(" -> ").append(adjacencyList.get(v)).append("\n");
        }
        
        return sb.toString();
    }
}
