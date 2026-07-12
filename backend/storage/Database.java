package storage;

import models.User;
import models.Group;
import models.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Database {
    private static String USERS_FILE = "data/users.txt";
    private static String GROUPS_FILE = "data/groups.txt";
    private static String MESSAGES_FOLDER = "data/messages/";

    private static String setToText(HashSet<String> set) {
        if (set.isEmpty())
        return "none";
        String result = "";
        int i = 0;
        for (String item : set) {
            if (i > 0)
                result += ",";
            result += item;
            i++;
        }
        return result;
    }

    private static void addAllToSet(HashSet<String> set, String text) {
        if (text.equals("none"))
            return;
        String[] items = text.split(",");
        for (String item : items) {
            set.add(item);
        }
    }

    public static void saveUsers(HashMap<String, User> users) {
        File file = new File(USERS_FILE);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (User user : users.values()) {
                writer.write(userToLine(user));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save users file");
        }
    }

    public static HashMap<String, User> loadUsers() {
        HashMap<String, User> users = new HashMap<>();
        File file = new File(USERS_FILE);
        if (!file.exists())
            return users;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = lineToUser(line);
                if (user != null)
                    users.put(user.getUserId(), user);
            }
        } catch (IOException e) {
            System.err.println("Could not read users file");
        }
        return users;
    }

    private static String userToLine(User user) {
        String contacts = setToText(user.getContacts());
        String blocked = setToText(user.getBlockedUserIds());
        String pinned = setToText(user.getPinnedChatIds());
        String archived = setToText(user.getArchivedChatIds());
        String muted = setToText(user.getMutedChatIds());

        return user.getUserId() + "|" + user.getUsername() + "|" + user.getPassword() + "|" +
                user.getProfilePicturePath() + "|" + user.isBlocked() + "|" +
                user.getFailedLoginAttempts() + "|" + user.getBlockUntil() + "|" +
                contacts + "|" + blocked + "|" + pinned + "|" + archived + "|" + muted;
    }

    private static User lineToUser(String line) {
        String[] parts = line.split("\\|");
        String userId = parts[0];
        String username = parts[1];
        String password = parts[2];
        String pic = parts[3];
        boolean isBlocked = Boolean.parseBoolean(parts[4]);
        int failedAttempts = Integer.parseInt(parts[5]);
        long blockUntil = Long.parseLong(parts[6]);

        User user = new User(username, password, userId);
        user.setProfilePicturePath(pic);
        user.setBlocked(isBlocked);
        user.setFailedLoginAttempts(failedAttempts);
        user.setBlockUntil(blockUntil);
        addAllToSet(user.getContacts(), parts[7]);
        addAllToSet(user.getBlockedUserIds(), parts[8]);
        addAllToSet(user.getPinnedChatIds(), parts[9]);
        addAllToSet(user.getArchivedChatIds(), parts[10]);
        addAllToSet(user.getMutedChatIds(), parts[11]);
        return user;
    }

    public static void saveGroups(HashMap<String, Group> groups) {
        File file = new File(GROUPS_FILE);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Group group : groups.values()) {
                writer.write(groupToLine(group));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save groups file");
        }
    }

    public static HashMap<String, Group> loadGroups() {
        HashMap<String, Group> groups = new HashMap<>();
        File file = new File(GROUPS_FILE);
        if (!file.exists())
            return groups;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Group group = lineToGroup(line);
                if (group != null)
                    groups.put(group.getGroupId(), group);
            }
        } catch (IOException e) {
            System.err.println("Could not read groups file");
        }
        return groups;
    }

    private static String groupToLine(Group group) {
        String members = setToText(group.getMembers());
        String admins = setToText(group.getAdminIds());
        return group.getGroupId() + "|" + group.getGroupName() + "|" +
                group.getProfilePicturePath() + "|" + group.getCreatorId() + "|" +
                members + "|" + admins;
    }

    private static Group lineToGroup(String line) {
        String[] parts = line.split("\\|");
        String groupId = parts[0];
        String groupName = parts[1];
        String pic = parts[2];
        String creatorId = parts[3];

        Group group = new Group(groupId, groupName, creatorId);
        group.setProfilePicturePath(pic);
        addAllToSet(group.getMembers(), parts[4]);
        addAllToSet(group.getAdminIds(), parts[5]);

        return group;
    }

    public static void saveMessages(String id, ArrayList<Message> messages) {
        File file = new File(MESSAGES_FOLDER + "msg_" + id + ".txt");
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Message message : messages) {
                writer.write(messageToLine(message));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save messages file");
        }
    }

    public static ArrayList<Message> loadMessages(String id) {
        ArrayList<Message> messages = new ArrayList<>();
        File file = new File(MESSAGES_FOLDER + "msg_" + id + ".txt");
        if (!file.exists())
            return messages;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Message message = lineToMessage(line);
                if (message != null)
                    messages.add(message);
            }
        } catch (IOException e) {
            System.err.println("Could not read messages file");
        }
        return messages;
    }

    private static String messageToLine(Message message) {
        return message.getMessageId() + "|" + message.getSenderId() + "|" +
                message.getContent() + "|" + message.getTimestamp() + "|" +
                message.isEdited() + "|" + message.isDeleted() + "|" + message.isReported();
    }

    private static Message lineToMessage(String line) {
        String[] parts = line.split("\\|");
        String messageId = parts[0];
        String senderId = parts[1];
        String content = parts[2];
        long timestamp = Long.parseLong(parts[3]);
        boolean isEdited = false;
        boolean isDeleted = false;
        boolean isReported = false;
        if (parts[4].equals("true"))
            isEdited = true;
        if (parts[5].equals("true")) 
            isDeleted = true;
        if (parts[6].equals("true"))
            isReported = true;

        Message message = new Message(messageId, senderId, content, null);
        message.setTimestamp(timestamp);
        message.setEdited(isEdited);
        message.setDeleted(isDeleted);
        message.setReported(isReported);

        return message;
    }
}