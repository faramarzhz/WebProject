package models;
import java.util.ArrayList;
import java.util.HashSet;

public class Group {
    private String groupId;
    private String name;
    private String profilePath; 
    private HashSet<String> members; 
    private ArrayList<Message> messages;
    private String creatorId; 
    private HashSet<String> adminIds;

    public Group(String groupId, String groupName, String creatorId) {
        this.groupId = groupId;
        this.name = groupName;
        profilePath = "";
        members = new HashSet<>();
        messages = new ArrayList<>();
        this.creatorId = creatorId;
        adminIds = new HashSet<>();
        adminIds.add(creatorId);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String groupName) {
        this.name = groupName;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePicturePath) {
        this.profilePath = profilePicturePath;
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
        if (!userId.equals(creatorId))
            adminIds.remove(userId);
    }
}