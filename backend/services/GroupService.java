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
        for (Group group : groups.values()) {
            ArrayList<Message> messages = Database.loadMessages(group.getGroupId());
            group.getMessages().addAll(messages);
        }
    }

    public Group createGroup(String groupId, String groupName, String creatorId) {
        Group newGroup = new Group(groupId, groupName, creatorId);
        newGroup.getMembers().add(creatorId);
        groups.put(groupId, newGroup);
        Database.saveGroups(groups);
        return newGroup;
    }

    public void addMember(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.getMembers().add(userId);
            Database.saveGroups(groups);
        }
    }

    public void removeMember(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.getMembers().remove(userId);
            Database.saveGroups(groups);
        }
    }

    public void updateGroupInfo(String groupId, String newGroupName, String newPic) {
        Group group = groups.get(groupId);
        if (group != null) {
            if (newGroupName != null && !newGroupName.isEmpty())
                group.setGroupName(newGroupName);
            if (newPic != null)
                group.setProfilePicturePath(newPic);
            Database.saveGroups(groups);
        }
    }

    public Group getGroupById(String groupId) {
        return groups.get(groupId);
    }

    public HashMap<String, Group> getAllGroups() {
        return groups;
    }

    public ArrayList<Group> getGroupsForUser(String userId) {
        ArrayList<Group> result = new ArrayList<>();
        for (Group g : groups.values()) {
            if (g.getMembers().contains(userId))
                result.add(g);
        }
        return result;
    }

    public boolean isGroupAdmin(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group == null)
            return false;
        return group.isAdmin(userId);
    }

    public void addAdmin(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.addAdmin(userId);
            Database.saveGroups(groups);
        }
    }

    public void removeAdmin(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.removeAdmin(userId);
            Database.saveGroups(groups);
        }
    }
}