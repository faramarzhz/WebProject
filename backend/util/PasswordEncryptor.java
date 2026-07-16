package util;

public class PasswordEncryptor {
    public static String hashPassword(String password) {
        if (password == null)
            return "";
        String orhi = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*";
        String fake = "zxywutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA9876543210*&^%$#@!";
        String result = "";
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            int index = orhi.indexOf(ch);
            result += fake.charAt(index);
        }
        return result;
    }
}