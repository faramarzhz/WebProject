package services;

import models.User;
import util.PasswordEncryptor;
import database.Database;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthService {
    private HashMap<String, User> users;

    public AuthService(HashMap<String, User> users) {
        this.users = users;
    }

    public boolean register(String username, String password, String userId) {
        if (users.containsKey(userId))
            return false;
        if (!validatePassword(password))
            return false;
        if (password.toLowerCase().contains(username.toLowerCase()))
            return false;
        User newUser = new User(username, PasswordEncryptor.hashPassword(password), userId);
        users.put(userId, newUser);
        Database.saveUsers(users);
        return true;
    }

    public User login(String userId, String password) {
        User user = users.get(userId);
        if (user == null)
            return null;
        long now = System.currentTimeMillis();
        if (user.isBlocked() && now < user.getBlockUntil())
            return null;
        if (user.isBlocked() && now >= user.getBlockUntil()) {
            user.setBlocked(false);
            user.setFailedLoginAttempts(0);
        }
        if (user.getPassword().equals(PasswordEncryptor.hashPassword(password))) {
            user.setFailedLoginAttempts(0);
            Database.saveUsers(users);
            return user;
        } else {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setBlocked(true);
                user.setBlockUntil(now + 300000);
            }
            Database.saveUsers(users);
            return null;
        }
    }

    public boolean isBlocked(String userId) {
        User user = users.get(userId);
        if (user == null)
            return false;
        return user.isBlocked() && System.currentTimeMillis() < user.getBlockUntil();
    }

    private boolean validatePassword(String password) {
        if (password == null)
            return false;
        int lengthOfPass = password.length();
        if (lengthOfPass < 8)
            return false;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        Pattern p1 = Pattern.compile("[A-Z]");
        Matcher m1 = p1.matcher(password);
        Pattern p2 = Pattern.compile("[a-z]");
        Matcher m2 = p2.matcher(password);
        Pattern p3 = Pattern.compile("[0-9]");
        Matcher m3 = p3.matcher(password);
        Pattern p4 = Pattern.compile("[!@#$%^&*]");
        Matcher m4 = p4.matcher(password);

        while (m1.find()) {
            hasUpper = true;
            break;
        }
        while (m2.find()) {
            hasLower = true;
            break;
        }
        while (m3.find()) {
            hasDigit = true;
            break;
        }
        while (m4.find()) {
            hasSpecial = true;
            break;
        }
        if (hasUpper && hasLower)
            if (hasDigit && hasSpecial)
                return true;
        return false;
    }
}