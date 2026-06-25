package models;

import java.util.HashSet;

public class User {
    private String username;
    private String password;
    private String userId;
    private String profilePicturePath;
    private boolean isBlocked; // مسدودی موقت یوزر
    private int failedLoginAttempts; // تعداد دفعاتی که کاربر رمز اشتباه زده
    private long blockUntil; // زمان پایان محرومیت کاربر(میلی‌ثانیه)
    private HashSet<String> contacts;// هش‌ست برای سرعت بالای سرچ کردن و تکراری نبودن

    public User(String username, String password, String userId) {
        this.username = username;
        this.password = password;
        this.userId = userId;
        profilePicturePath = "";
        isBlocked = false;
        failedLoginAttempts = 0;
        blockUntil = 0;
        contacts = new HashSet<>();
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public long getBlockUntil() {
        return blockUntil;
    }

    public void setBlockUntil(long blockUntil) {
        this.blockUntil = blockUntil;
    }

    public HashSet<String> getContacts() {
        return contacts;
    }
}