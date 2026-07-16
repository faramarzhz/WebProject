package services;
import models.Group;
import models.Message;
import database.Database;
import java.util.ArrayList;
import java.util.HashMap;

public class GroupService {
    private HashMap<String, Group> groups;

    public GroupService() {
        groups = Database.loadGroups();
        for (Group grop : groups.values()) {
            ArrayList<Message> messages = Database.loadMessages(grop.getGroupId());
            grop.getMessages().addAll(messages);
        }
    }

    public boolean isGroupAdmin(String groupId, String userId) {
        Group grop = groups.get(groupId);
        if (grop == null)
            return false;
        return grop.isAdmin(userId);
    }

    public void removeMember(String groupId, String userId) {
        Group grop = groups.get(groupId);
        if (grop != null) {
            grop.getMembers().remove(userId);
            Database.saveGroups(groups);
        }
    }

    public void addAdmin(String groupId, String userId) {
        Group grop = groups.get(groupId);
        if (grop != null) {
            grop.addAdmin(userId);
            Database.saveGroups(groups);
        }
    }

    public void updateGroupInfo(String groupId, String newGroupName, String newPic) {
        Group grop = groups.get(groupId);
        if (grop != null) {
            if (newPic != null)
                grop.setProfilePath(newPic);
            if (newGroupName != null && !newGroupName.isEmpty())
                grop.setName(newGroupName);
            Database.saveGroups(groups);
        }
    }

    public Group createGroup(String groupId, String groupName, String creatorId) {
        Group newgrop = new Group(groupId, groupName, creatorId);
        newgrop.getMembers().add(creatorId);
        groups.put(groupId, newgrop);
        Database.saveGroups(groups);
        return newgrop;
    }

    public ArrayList<Group> getGroupsForUser(String userId) {
        ArrayList<Group> result = new ArrayList<>();
        for (Group grop : groups.values()) {
            if (grop.getMembers().contains(userId))
                result.add(grop);
        }
        return result;
    }

    public void removeAdmin(String groupId, String userId) {
        Group grop = groups.get(groupId);
        if (grop != null) {
            grop.removeAdmin(userId);
            Database.saveGroups(groups);
        }
    }

    public Group getGroupById(String groupId) {
        return groups.get(groupId);
    }

    public void addMember(String groupId, String userId) {
        Group grop = groups.get(groupId);
        if (grop != null) {
            grop.getMembers().add(userId);
            Database.saveGroups(groups);
        }
    }

    public HashMap<String, Group> getAllGroups() {
        return groups;
    }
}