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

    public ArrayList<Chat> getChatsForUser(String userId) {
        ArrayList<Chat> result = new ArrayList<>();
        for (Chat chat : chats.values()) {
            if (chat.getUsers().contains(userId))
                result.add(chat);
        }
        return result;
    }

    public HashMap<String, Chat> getAllChats() {
        return chats;
    }

    public Chat getOrCreateSavedMessages(String userId) {
        String savedId = "saved_" + userId;
        if (!chats.containsKey(savedId)) {
            Chat saved = new Chat(savedId, userId, userId);
            saved.setType("private");
            saved.setName("Saved Messages");
            chats.put(savedId, saved);
            Database.saveChats(chats);
            return saved;
        } else {
            return chats.get(savedId);
        }
    }

    public Chat getChatById(String chatId) {
        return chats.get(chatId);
    }

    public ArrayList<Message> getChatHistory(String chatId) {
        Chat chat = chats.get(chatId);
        if (chat == null) {
            return new ArrayList<>();
        } else {
            return chat.getMessages();
        }
    }

    public Chat createChat(String chatId, String user1, String user2) {
        if (!chats.containsKey(chatId)) {
            Chat newChat = new Chat(chatId, user1, user2);
            chats.put(chatId, newChat);
            Database.saveChats(chats);
            return newChat;
        }
        return chats.get(chatId);
    }

    public Chat findPrivateChat(String user1, String user2) {
        for (Chat chat : chats.values()) {
            if ("private".equals(chat.getType()) && chat.getUsers().contains(user1)
                    && chat.getUsers().contains(user2)) {
                return chat;
            }
        }
        return null;
    }
}