package services;
import models.User;
import database.Database;
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

    public boolean changeUserId(String ghadimi, String jadid) {
        if (users.containsKey(jadid))
            return false;
        User user = users.remove(ghadimi);
        if (user == null)
            return false;
        user.setUserId(jadid);
        users.put(jadid, user);
        Database.saveUsers(users);
        return true;
    }

    public void updateProfile(String ghadimi, String jadid, String proPath) {
        User user = users.get(ghadimi);
        if (user != null) {
            if (jadid != null && !jadid.isEmpty())
                user.setUsername(jadid);
            if (proPath != null)
                user.setProfilePath(proPath);
            Database.saveUsers(users);
        }
    }

    public void addContact(String userId, String conttacid) {
        User user = users.get(userId);
        if (user != null && users.containsKey(conttacid)) {
            user.getContacts().add(conttacid);
            Database.saveUsers(users);
        }
    }

    public void blockUser(String userId, String blok) {
        User user = users.get(userId);
        if (user != null && users.containsKey(blok)) {
            user.getBlockUser().add(blok);
            Database.saveUsers(users);
        }
    }

    public void pinChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getPinChat().add(chatId);
            Database.saveUsers(users);
        }
    }
    public void archiveChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getArchiveChat().add(chatId);
            Database.saveUsers(users);
        }
    }
    public void muteChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getMuteChat().add(chatId);
            Database.saveUsers(users);
        }
    }
    public void unarchiveChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getArchiveChat().remove(chatId);
            Database.saveUsers(users);
        }
    }
    public void unmuteChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getMuteChat().remove(chatId);
            Database.saveUsers(users);
        }
    }
    public void deleteAccount(String userId) {
        users.remove(userId);
        Database.saveUsers(users);
    }
    public void unblockUser(String userId, String blok) {
        User user = users.get(userId);
        if (user != null) {
            user.getBlockUser().remove(blok);
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
    public void unpinChat(String userId, String chatId) {
        User user = users.get(userId);
        if (user != null) {
            user.getPinChat().remove(chatId);
            Database.saveUsers(users);
        }
    }
}