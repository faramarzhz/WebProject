package models;

import java.util.ArrayList;
import java.util.HashSet;

public class Group {
    private String groupId;
    private String groupName;
    private String profilePicturePath; // برای فاز دو
    private HashSet<String> members; // هش‌ست برای سرعت بالا و تکراری نبودن
    private ArrayList<Message> messages;// ارای لیست بخاطر حفظ کردن ترتیب

    private String creatorId; 
    private HashSet<String> adminIds;

    public Group(String groupId, String groupName, String creatorId) {
        this.groupId = groupId;
        this.groupName = groupName;
        profilePicturePath = "";
        members = new HashSet<>();
        messages = new ArrayList<>();
        this.creatorId = creatorId;
        adminIds = new HashSet<>();
        adminIds.add(creatorId);
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    public HashSet<String> getMembers() {
        return members;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public HashSet<String> getAdminIds() {
        return adminIds;
    }

    public boolean isAdmin(String userId) {
        return adminIds.contains(userId);
    }

    public void addAdmin(String userId) {
        adminIds.add(userId);
    }

    public void removeAdmin(String userId) {
        if (!userId.equals(creatorId)) {
            adminIds.remove(userId);
        }
    }
}