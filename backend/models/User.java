package models;
import java.util.HashSet;

public class User {
    private String username;
    private String password;
    private String userId;
    private String profilePath;
    private boolean isBlocked; 
    private int failLogin;
    private long blockUntil; 
    private HashSet<String> contacts;
    private long lastSeen;
    private HashSet<String> blockUser;
    private HashSet<String> pinChat;
    private HashSet<String> archiveChat;
    private HashSet<String> muteChat;

    public User(String username, String password, String userId) {
        this.username = username;
        this.password = password;
        this.userId = userId;
        profilePath = "";
        isBlocked = false;
        failLogin = 0;
        blockUntil = 0;
        contacts = new HashSet<>();
        blockUser = new HashSet<>();
        pinChat = new HashSet<>();
        archiveChat = new HashSet<>();
        muteChat = new HashSet<>();
    }

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

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePicturePath) {
        this.profilePath = profilePicturePath;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public int getFailLogin() {
        return failLogin;
    }

    public void setFailLogin(int failedLoginAttempts) {
        this.failLogin = failedLoginAttempts;
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

    public HashSet<String> getBlockUser() {
        return blockUser;
    }

    public boolean hasBlocked(String otherUserId) {
        return blockUser.contains(otherUserId);
    }

    public HashSet<String> getPinChat() {
        return pinChat;
    }

    public HashSet<String> getArchiveChat() {
        return archiveChat;
    }

    public HashSet<String> getMuteChat() {
        return muteChat;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}