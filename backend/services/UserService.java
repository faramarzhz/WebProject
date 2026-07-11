package services;

import models.User;
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
        return true;
    }

    public void updateProfile(String userId, String newUsername, String newProfilePicturePath) {
        User user = users.get(userId);
        if (user != null) {
            if (newUsername != null)
                user.setUsername(newUsername);
            if (newProfilePicturePath != null)
                user.setProfilePicturePath(newProfilePicturePath);
        }
    }

    public void deleteAccount(String userId) {
        users.remove(userId);
    }

    public void addContact(String userId, String contactId) {
        User user = users.get(userId);
        if (user != null && users.containsKey(contactId))
            user.getContacts().add(contactId);
    }

    public void removeContact(String userId, String contactId) {
        User user = users.get(userId);
        if (user != null)
            user.getContacts().remove(contactId);
    }

    public void blockUser(String userId, String targetId) {
        User user = users.get(userId);
        if (user != null && users.containsKey(targetId))
            user.getBlockedUserIds().add(targetId);
    }

    public void unblockUser(String userId, String targetId) {
        User user = users.get(userId);
        if (user != null)
            user.getBlockedUserIds().remove(targetId);
    }

    public void pinChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null)
            user.getPinnedChatIds().add(chatId);
    }

    public void unpinChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null)
            user.getPinnedChatIds().remove(chatId);
    }

    public void archiveChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null)
            user.getArchivedChatIds().add(chatId);
    }

    public void unarchiveChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null)
            user.getArchivedChatIds().remove(chatId);
    }

    public void muteChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null)
            user.getMutedChatIds().add(chatId);
    }

    public void unmuteChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null)
            user.getMutedChatIds().remove(chatId);
    }
}