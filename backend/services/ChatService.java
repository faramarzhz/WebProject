package services;

import models.Chat;
import models.Message;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatService {
    private HashMap<String, Chat> chats = new HashMap<>(); // ذخیره کل چت‌های سرور,هش مپ برای دسترسی سریع با chatId
    private HashMap<String, String> savedMessagesMap = new HashMap<>(); // نگهداری رابطه بین userId و chatId مربوط به
                                                                        // بخش Saved Messages

    // متد ساخت چت خصوصی جدید(اگر از قبل نبود)
    public Chat createChat(String chatId, String user1, String user2) {
        if (chats.containsKey(chatId))
            return chats.get(chatId); // اگر چت از قبل وجود داشت همان را برمی‌گرداند
        Chat newChat = new Chat(chatId, user1, user2);
        chats.put(chatId, newChat);
        return newChat;
    }

    // جستجوی چت خصوصیِ موجود بین دو یوزر برای جلوگیری از ساخت چت تکراری
    public Chat findPrivateChat(String user1, String user2) {
        for (Chat chat : chats.values()) {
            if ("private".equals(chat.getType()) && chat.getParticipants().contains(user1)
                    && chat.getParticipants().contains(user2)) {
                return chat;
            }
        }
        return null;
    }

    // Saved Messages پیدا کردن یا ساختن چت با خود یوزر
    public Chat getOrCreateSavedMessages(String userId) {
        // اگر قبلاً ساخته شده همان را برمی‌گرداند
        if (savedMessagesMap.containsKey(userId)) {
            String savedId = savedMessagesMap.get(userId);
            return chats.get(savedId);
        }

        // اگر اولین بار بود چت اختصاصی با خود یوزر ساخته می‌شود
        String savedId = "saved_" + userId;
        Chat saved = new Chat(savedId, userId, userId); // ارسال آیدی یوزر به عنوان هردوطرف چت
        saved.setType("private");
        saved.setName("Saved Messages");
        chats.put(savedId, saved);
        savedMessagesMap.put(userId, savedId); // ذخیره آیدی چت در مپSaved Messages
        return saved;
    }

    // گرفتن یک چت خاص بر اساس آیدی آن
    public Chat getChatById(String chatId) {
        return chats.get(chatId);
    }

    // گرفتن تاریخچه کامل پیام‌های یک چت به صورت آرایه مرتب‌شده
    public ArrayList<Message> getChatHistory(String chatId) {
        Chat chat = chats.get(chatId);
        return chat != null ? chat.getMessages() : new ArrayList<>();
    }

    // فیلتر کردن و استخراج تمام چت‌هایی که یک یوزر مشخص در آن‌ها عضویت دارد
    public ArrayList<Chat> getChatsForUser(String userId) {
        ArrayList<Chat> result = new ArrayList<>();
        for (Chat chat : chats.values()) {
            if (chat.getParticipants().contains(userId))
                result.add(chat);
        }
        return result;
    }

    // دسترسی مستقیم به کل مپ چت‌ها
    public HashMap<String, Chat> getAllChats() {
        return chats;
    }
}