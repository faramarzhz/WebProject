package util;
import java.security.MessageDigest;
import java.util.Base64;

public class WebSocketKeyGenerator {
    public static String generateAcceptKey(String kelidclient) {
        String standardemorgar = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        try {
            kelidclient += standardemorgar;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(kelidclient.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}