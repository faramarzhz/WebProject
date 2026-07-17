package util;

public class MessageEncryptor {
    private static int shift = 5;
    public static String encrypt(String content) {
        String result = "";
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            int newCode = (int) ch + 5;
            result += (char) newCode;
        }
        return result;
    }
    public static String decrypt(String encryptContent) {
        String result = "";
        for (int i = 0; i < encryptContent.length(); i++) {
            char ch = encryptContent.charAt(i);
            int newCode = (int) ch - shift;
            result += (char) newCode;
        }
        return result;
    }
}