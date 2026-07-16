package util;
import java.security.MessageDigest;
import java.util.Base64;

public class WebSocketKeyGenerator {
    public static String generateAcceptKey(String clientKey) {
        String standardemorgar = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        try {
            String combine = clientKey + standardemorgar;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combine.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}