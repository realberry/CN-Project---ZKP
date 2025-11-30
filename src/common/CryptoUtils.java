package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class CryptoUtils {
    
    private static final SecureRandom random = new SecureRandom();
    
    // Generate a random nonce as a hexadecimal string
    public static String generateNonce() {
        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        return bytesToHex(nonce);
    }
    
    // Create a commitment by hashing the colour with the nonce
    public static String createCommitment(String colour, String nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = colour + ":" + nonce;
            byte[] hash = digest.digest(data.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    // Verify that a commitment matches the revealed colour and nonce
    public static boolean verifyCommitment(String commitment, String colour, String nonce) {
        String recomputed = createCommitment(colour, nonce);
        return commitment.equals(recomputed);
    }
    
    // Generate a random permutation of colours
    public static Map<String, String> generateColourPermutation(Set<String> colours) {
        List<String> colourList = new ArrayList<>(colours);
        List<String> permuted = new ArrayList<>(colours);
        Collections.shuffle(permuted, random);
        
        Map<String, String> permutation = new HashMap<>();
        for (int i = 0; i < colourList.size(); i++) {
            permutation.put(colourList.get(i), permuted.get(i));
        }
        
        return permutation;
    }
    
    // Convert byte array to hexadecimal string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
