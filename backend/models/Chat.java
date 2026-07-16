package models;

import java.util.ArrayList;

public class Chat {
    private String chatId;
    private String type; 
    private ArrayList<String> participants;
    private ArrayList<Message> messages;
    private String name;

    public Chat(String chatId, String user1, String user2) {
        this.chatId = chatId;
        type = "private"; 
        participants = new ArrayList<>();
        participants.add(user1);
        participants.add(user2);
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

    public ArrayList<String> getParticipants() {
        return participants;
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