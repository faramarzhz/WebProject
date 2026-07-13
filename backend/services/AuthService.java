package services;

import models.User;
import util.PasswordEncryptor;
import database.Database;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthService {
    private HashMap<String, User> users; // هش مپ برای سرعت بالا درجستجوی آیدی

    public AuthService(HashMap<String, User> users) {
        this.users = users;
    }

    // ثبت‌نام یوزر جدید
    public boolean register(String username, String password, String userId) {
        if (users.containsKey(userId))
            return false; // اگر آیدی تکراری بود ثبت‌ نام لغو می‌شود
        if (!validatePassword(password))
            return false; // اگر پسورد ضعیف بود ثبت‌ نام لغو می‌شود
        if (password.toLowerCase().contains(username.toLowerCase()))
            return false; // عدم وجود نام کاربری در پسورد

        // رمز عبور ابتدا هش شده و بعد شی یوزر ساخته می‌شود
        User newUser = new User(username, PasswordEncryptor.hashPassword(password), userId);
        users.put(userId, newUser);
        Database.saveUsers(users);
        return true;
    }

    // لاگین همراه با کنترل تلاش‌ های ناموفق
    public User login(String userId, String password) {
        User user = users.get(userId);
        if (user == null)
            return null;

        long now = System.currentTimeMillis();
        // بررسی اینکه کاربر هنوز در دوره محرومیت 5 دقیقه‌ای قرار داره یا نه
        if (user.isBlocked() && now < user.getBlockUntil())
            return null;
        // اگر زمان محرومیت تمام شده باشد، قفل حساب باز می‌شود
        if (user.isBlocked() && now >= user.getBlockUntil()) {
            user.setBlocked(false);
            user.setFailedLoginAttempts(0);
        }
        // بررسی رمز عبور هش‌شده
        if (user.getPassword().equals(PasswordEncryptor.hashPassword(password))) {
            user.setFailedLoginAttempts(0); // صفر کردن شمارنده خطاهای پسورد پس از لاگین موفق
            Database.saveUsers(users);
            return user;
        } else {
            // افزایش تعداد خطاهای پسورد در صورت اشتباه بودن رمز
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setBlocked(true);
                user.setBlockUntil(now + (5 * 60000)); // محرومیت دقیقاً به مدت 5 دقیقه (پایه فاز دو)
            }
            Database.saveUsers(users);
            return null;
        }
    }

    // چک کردن وضعیت مسدود بودن کاربر
    public boolean isBlocked(String userId) {
        User user = users.get(userId);
        if (user == null)
            return false;
        return user.isBlocked() && System.currentTimeMillis() < user.getBlockUntil();
    }

    // تایید قدرت رمز عبور
    private boolean validatePassword(String password) {
        // شرط اول: حداقل 8 کاراکتر باشد
        if (password.length() < 8) {
            return false;
        }

        // pattern الگو
        // حروف بزرگ
        Pattern upperPattern = Pattern.compile("[A-Z]");
        Matcher upperMatcher = upperPattern.matcher(password);
        boolean hasUpper = upperMatcher.find();
        // حروف کوچیک
        Pattern lowerPattern = Pattern.compile("[a-z]");
        Matcher lowerMatcher = lowerPattern.matcher(password);
        boolean hasLower = lowerMatcher.find();
        // اعداد
        Pattern digitPattern = Pattern.compile("[0-9]");
        Matcher digitMatcher = digitPattern.matcher(password);
        boolean hasDigit = digitMatcher.find();
        // کاراکترها
        Pattern specialPattern = Pattern.compile("[!@#$%^&*]");
        Matcher specialMatcher = specialPattern.matcher(password);
        boolean hasSpecial = specialMatcher.find();

        if (hasUpper && hasLower && hasDigit && hasSpecial) {
            return true;
        } else {
            return false;
        }
    }
}