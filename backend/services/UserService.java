package services;

import models.User;
import storage.Database;
import java.util.HashMap;

public class UserService {
    private HashMap<String, User> users;

    public UserService(HashMap<String, User> users) {
        this.users = users;
    }

    public User getUserById(String userId) {
        return users.get(userId);
    }

    public boolean userExists(String userId) {
        return users.containsKey(userId);
    }

    public boolean changeUserId(String oldId, String newId) {
        if (users.containsKey(newId))
            return false;
        User user = users.remove(oldId);
        if (user == null)
            return false;
        user.setUserId(newId);
        users.put(newId, user);
        Database.saveUsers(users);
        return true;
    }

    public void updateProfile(String userId, String newUsername, String newProfilePicturePath) {
        User user = users.get(userId);
        if (user != null) {
            if (newUsername != null && !newUsername.isEmpty())
                user.setUsername(newUsername);
            if (newProfilePicturePath != null)
                user.setProfilePicturePath(newProfilePicturePath);
            Database.saveUsers(users);
        }
    }

    public void deleteAccount(String userId) {
        users.remove(userId);
        Database.saveUsers(users);
    }

    public void addContact(String userId, String contactId) {
        User user = users.get(userId);
        if (user != null && users.containsKey(contactId)) {
            user.getContacts().add(contactId);
            Database.saveUsers(users);
        }
    }

    public void removeContact(String userId, String contactId) {
        User user = users.get(userId);
        if (user != null) {
            user.getContacts().remove(contactId);
            Database.saveUsers(users);
        }
    }

    public void blockUser(String userId, String targetId) {
        User user = users.get(userId);
        if (user != null && users.containsKey(targetId)) {
            user.getBlockedUserIds().add(targetId);
            Database.saveUsers(users);
        }
    }

    public void unblockUser(String userId, String targetId) {
        User user = users.get(userId);
        if (user != null) {
            user.getBlockedUserIds().remove(targetId);
            Database.saveUsers(users);
        }
    }

    public void pinChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getPinnedChatIds().add(chatId);
            Database.saveUsers(users);
        }
    }

    public void unpinChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getPinnedChatIds().remove(chatId);
            Database.saveUsers(users);
        }
    }

    public void archiveChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getArchivedChatIds().add(chatId);
            Database.saveUsers(users);
        }
    }

    public void unarchiveChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getArchivedChatIds().remove(chatId);
            Database.saveUsers(users);
        }
    }

    public void muteChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getMutedChatIds().add(chatId);
            Database.saveUsers(users);
        }
    }

    public void unmuteChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getMutedChatIds().remove(chatId);
            Database.saveUsers(users);
        }
    }
}