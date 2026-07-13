package services;

import models.Chat;
import models.Message;
import database.Database;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatService {
    private HashMap<String, Chat> chats;

    public ChatService() {
        chats = Database.loadChats();
        for (Chat chat : chats.values()) {
            ArrayList<Message> messages = Database.loadMessages(chat.getChatId());
            chat.getMessages().addAll(messages);
        }
    }

    public Chat createChat(String chatId, String user1, String user2) {
        if (chats.containsKey(chatId))
            return chats.get(chatId);
        Chat newChat = new Chat(chatId, user1, user2);
        chats.put(chatId, newChat);
        Database.saveChats(chats);
        return newChat;
    }

    public Chat findPrivateChat(String user1, String user2) {
        for (Chat chat : chats.values()) {
            if ("private".equals(chat.getType()) && chat.getParticipants().contains(user1)
                    && chat.getParticipants().contains(user2)) {
                return chat;
            }
        }
        return null;
    }

    public Chat getOrCreateSavedMessages(String userId) {
        String savedId = "saved_" + userId;
        if (chats.containsKey(savedId)) {
            return chats.get(savedId);
        }

        Chat saved = new Chat(savedId, userId, userId);
        saved.setType("private");
        saved.setName("Saved Messages");
        chats.put(savedId, saved);
        Database.saveChats(chats);
        return saved;
    }

    public Chat getChatById(String chatId) {
        return chats.get(chatId);
    }

    public ArrayList<Message> getChatHistory(String chatId) {
        Chat chat = chats.get(chatId);
        return chat != null ? chat.getMessages() : new ArrayList<>();
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