package util;

public class IdGenerator {
    private static long counter = System.currentTimeMillis(); // استفاده از زمان جاری سیستم به عنوان پایه آیدی
    // متد synchronized برای تولید آیدی‌های غیرتکراری و یکتا در
    public static synchronized String generateId() {
        counter++;
        return String.valueOf(counter);
    }
}