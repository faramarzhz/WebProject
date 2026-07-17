package database;
import models.User;
import util.MessageEncryptor;
import models.Chat;
import models.Group;
import models.Message;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Database {
    private static String user = "data/users.txt";
    private static String group = "data/groups.txt";
    private static String message = "data/messages/";
    private static String chat = "data/chats.txt";

    private static String setToText(HashSet<String> s) {
        if (s.isEmpty())
            return "none";
        String result = "";
        int i = 0;
        for (String item : s) {
            if (i > 0)
                result += ",";
            result += item;
            i++;
        }
        return result;
    }

    private static void addAllToSet(HashSet<String> s, String text) {
        if (text.equals("none"))
            return;
        String[] items = text.split(",");
        for (String item : items) {
            s.add(item);
        }
    }

    public static void saveUsers(HashMap<String, User> users) {
        File file = new File(user);
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
        File file = new File(user);
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
        String contact = setToText(user.getContacts());
        String block = setToText(user.getBlockUser());
        String pin = setToText(user.getPinChat());
        String archiv = setToText(user.getArchiveChat());
        String mute = setToText(user.getMuteChat());
        return user.getUserId() + "|" + user.getUsername() + "|" + user.getPassword() + "|" +
                user.getProfilePath() + "|" + user.isBlocked() + "|" +
                user.getFailLogin() + "|" + user.getBlockUntil() + "|" +
                contact + "|" + block + "|" + pin + "|" + archiv + "|" + mute;
    }

    private static User lineToUser(String line) {
        String[] parts = line.split("\\|");
        String userId = parts[0];
        String username = parts[1];
        String password = parts[2];
        String pic = parts[3];
        boolean isBlocked = Boolean.parseBoolean(parts[4]);
        int failLogin = Integer.parseInt(parts[5]);
        long blockTime = Long.parseLong(parts[6]);
        User user = new User(username, password, userId);
        user.setProfilePath(pic);
        user.setBlocked(isBlocked);
        user.setFailLogin(failLogin);
        user.setBlockUntil(blockTime);
        addAllToSet(user.getContacts(), parts[7]);
        addAllToSet(user.getBlockUser(), parts[8]);
        addAllToSet(user.getPinChat(), parts[9]);
        addAllToSet(user.getArchiveChat(), parts[10]);
        addAllToSet(user.getMuteChat(), parts[11]);
        return user;
    }

    public static void saveGroups(HashMap<String, Group> groups) {
        File file = new File(group);
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
        File file = new File(group);
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
        return group.getGroupId() + "|" + group.getName() + "|" +
                group.getProfilePath() + "|" + group.getCreatorId() + "|" +
                members + "|" + admins;
    }

    private static Group lineToGroup(String line) {
        String[] parts = line.split("\\|");
        String groupId = parts[0];
        String groupName = parts[1];
        String pic = parts[2];
        String creatorId = parts[3];
        Group group = new Group(groupId, groupName, creatorId);
        group.setProfilePath(pic);
        addAllToSet(group.getMembers(), parts[4]);
        addAllToSet(group.getAdminIds(), parts[5]);
        return group;
    }

    public static void saveMessages(String id, ArrayList<Message> messages) {
        File file = new File(message + "msg_" + id + ".txt");
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
        File file = new File(message + "msg_" + id + ".txt");
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
        String encryptedContent = MessageEncryptor.encrypt(message.getContent());
        return message.getMessageId() + "|" + message.getSenderId() + "|" +
                encryptedContent + "|" + message.getTimestamp() + "|" +
                message.isEdited() + "|" + message.isDeleted() + "|" + message.isReported();
    }

    private static Message lineToMessage(String line) {
        String[] parts = line.split("\\|");
        String messageId = parts[0];
        String senderId = parts[1];
        String content = MessageEncryptor.decrypt(parts[2]);
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

    public static void saveChats(HashMap<String, Chat> chats) {
        File file = new File(chat);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Chat chat : chats.values()) {
                writer.write(chatToLine(chat));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save chats file");
        }
    }

    public static HashMap<String, Chat> loadChats() {
        HashMap<String, Chat> chats = new HashMap<>();
        File file = new File(chat);
        if (!file.exists())
            return chats;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Chat chat = lineToChat(line);
                if (chat != null)
                    chats.put(chat.getChatId(), chat);
            }
        } catch (IOException e) {
            System.err.println("Could not read chats file");
        }
        return chats;
    }

    private static String chatToLine(Chat chat) {
        ArrayList<String> parts = chat.getUsers();
        String participants = "";
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                participants += ",";
            participants += parts.get(i);
        }
        return chat.getChatId() + "|" + chat.getType() + "|" + chat.getName() + "|" + participants;
    }

    private static Chat lineToChat(String line) {
        String[] parts = line.split("\\|");
        String chatId = parts[0];
        String type = parts[1];
        String name = parts[2];
        String[] participants = parts[3].split(",");
        Chat chat = new Chat(chatId, participants[0], participants[1]);
        chat.setType(type);
        chat.setName(name);
        return chat;
    }
}