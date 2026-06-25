package util;

import java.security.MessageDigest;

public class PasswordEncryptor {
    // یک‌طرفه کردن و رمزنگاری ایمن رمز عبور با الگوریتم SHA-256
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b)); // تبدیل بایت‌ها به رشته هگزادسیمال
            }
            return sb.toString();
        } catch (Exception e) {
            return password; // در صورت خطای سیستمی خود رمز را برمی‌گرداند
        }
    }
}