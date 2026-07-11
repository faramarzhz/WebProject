package services;

import models.User;
import util.PasswordEncryptor;
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
            return user;
        } else {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setBlocked(true);
                user.setBlockUntil(now + (5 * 60000));
            }
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
        if (password.length() < 8)
            return false;

        Pattern upperPattern = Pattern.compile("[A-Z]");
        Matcher upperMatcher = upperPattern.matcher(password);
        boolean hasUpper = upperMatcher.find();
        Pattern lowerPattern = Pattern.compile("[a-z]");
        Matcher lowerMatcher = lowerPattern.matcher(password);
        boolean hasLower = lowerMatcher.find();
        Pattern digitPattern = Pattern.compile("[0-9]");
        Matcher digitMatcher = digitPattern.matcher(password);
        boolean hasDigit = digitMatcher.find();
        Pattern specialPattern = Pattern.compile("[!@#$%^&*]");
        Matcher specialMatcher = specialPattern.matcher(password);
        boolean hasSpecial = specialMatcher.find();

        if (hasUpper && hasLower && hasDigit && hasSpecial) 
            return true;
        return false;
    }
}