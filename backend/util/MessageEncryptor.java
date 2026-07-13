package util;

public class MessageEncryptor {
    private static int shiftAmount = 5;

    public static String encrypt(String content) {
        String result = "";
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            int newCode = (int) ch + shiftAmount;
            result += (char) newCode;
        }
        return result;
    }

    public static String decrypt(String encryptedContent) {
        String result = "";
        for (int i = 0; i < encryptedContent.length(); i++) {
            char ch = encryptedContent.charAt(i);
            int newCode = (int) ch - shiftAmount;
            result += (char) newCode;
        }
        return result;
    }
}