package models;
import java.util.ArrayList;

public class Chat {
    private String chatId;
    private String type; 
    private ArrayList<String> users;
    private ArrayList<Message> messages;
    private String name;

    public Chat(String chatId, String user1, String user2) {
        this.chatId = chatId;
        type = "private"; 
        users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        messages = new ArrayList<>();
        name = ""; 
    }

    public String getChatId() {
        return chatId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}