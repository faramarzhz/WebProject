package util;

import java.security.MessageDigest;
import java.util.Base64;

public class WebSocketKeyGenerator {
    private static String magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static String generateAcceptKey(String clientKey) {
        try {
            String combined = clientKey + magicString;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.err.println("Could not generate WebSocket accept key");
            return "";
        }
    }
}