package models;

import java.util.ArrayList;

public class Chat {
    private String chatId;
    private String type; // تایپ چت
    private ArrayList<String> participants;//برای مدیریت ساده وسبک مخاطبین دوطرفه
    private ArrayList<Message> messages;// ارای لیست بخاطر حفظ کردن ترتیب
    private String name;

    public Chat(String chatId, String user1, String user2) {
        this.chatId = chatId;
        type = "private"; // برای فاز 1 پیش فرض روی پرایوت میزاریم
        participants = new ArrayList<>();
        participants.add(user1);
        participants.add(user2);
        messages = new ArrayList<>();
        name = ""; // اسم چت(برای گروه ها)
    }

    // Getters and Setters
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