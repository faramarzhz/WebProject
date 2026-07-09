package services;

import models.Chat;
import models.Message;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatService {
    private HashMap<String, Chat> chats = new HashMap<>();
    private HashMap<String, String> savedMessagesMap = new HashMap<>();

    public Chat createChat(String chatId, String user1, String user2) {
        if (chats.containsKey(chatId))
            return chats.get(chatId);
        Chat newChat = new Chat(chatId, user1, user2);
        chats.put(chatId, newChat);
        return newChat;
    }

    public Chat findPrivateChat(String user1, String user2) {
        for (Chat chat : chats.values()) {
            if (chat.getType().equals("private") && chat.getParticipants().contains(user1) && chat.getParticipants().contains(user2))
                return chat;
        }
        return null;
    }

    public Chat getOrCreateSavedMessages(String userId) {
        if (savedMessagesMap.containsKey(userId)) {
            String savedId = savedMessagesMap.get(userId);
            return chats.get(savedId);
        }
        String savedId = "saved_" + userId;
        Chat saved = new Chat(savedId, userId, userId);
        saved.setType("private");
        saved.setName("Saved Messages");
        chats.put(savedId, saved);
        savedMessagesMap.put(userId, savedId);
        return saved;
    }

    public Chat getChatById(String chatId) {
        return chats.get(chatId);
    }

    public ArrayList<Message> getChatHistory(String chatId) {
        Chat chat = chats.get(chatId);
        if (chat != null)
            return chat.getMessages();
        return new ArrayList<>();
    }

    public ArrayList<Chat> getChatsForUser(String userId) {
        ArrayList<Chat> result = new ArrayList<>();
        for (Chat chat : chats.values()) {
            if (chat.getParticipants().contains(userId))
                result.add(chat);
        }
        return result;
    }

    public HashMap<String, Chat> getAllChats() {
        return chats;
    }
}