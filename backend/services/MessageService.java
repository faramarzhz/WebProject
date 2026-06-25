package services;

import models.Chat;
import models.Group;
import models.Message;
import java.util.HashMap;

public class MessageService {
    // ردیابی زمان و تعداد پیام برای جلوگیری از اسپم دو هش مپ برای مدیریت پنجره
    // زمانی هر کاربر
    private HashMap<String, Long> userWindowStart = new HashMap<>();
    private HashMap<String, Integer> userMessageCount = new HashMap<>();

    // ساخت پیام جدید با چک کردن اسپم
    public Message createMessage(String messageId, String senderId, String content, String mediaPath) {
        if (isSpam(senderId))
            return null;
        return new Message(messageId, senderId, content, mediaPath);
    }

    // اضافه کردن پیام به لیست پیام‌های چت خصوصی
    public void addMessageToChat(Chat chat, Message message) {
        if (chat != null && message != null)
            chat.getMessages().add(message);
    }

    // اضافه کردن پیام به لیست پیام‌های گروه
    public void addMessageToGroup(Group group, Message message) {
        if (group != null && message != null)
            group.getMessages().add(message);
    }

    // ویرایش پیام
    public void editMessage(Message message, String newContent) {
        if (message != null && !message.isDeleted()) {
            message.setContent(newContent);
            message.setEdited(true);
        }
    }

    // حذف پیام
    public void deleteMessage(Message message) {
        if (message != null) {
            message.setContent("This message was deleted.");
            message.setDeleted(true);
        }
    }

    // ریپورت کردن پیام‌ ها(برای ادمین)
    public void reportMessage(Message message) {
        if (message != null)
            message.setReported(true);
    }

    // حداکثر 5 پیام در هر ثانیه
    private boolean isSpam(String senderId) {
        long now = System.currentTimeMillis();
        long windowStart = userWindowStart.getOrDefault(senderId, 0L);

        if (now - windowStart > 1000) {
            // اگر بیشتر از 1 ثانیه گذشته باشد پنجره زمانی جدید باز شده و شمارنده 1 می‌شود
            userWindowStart.put(senderId, now);
            userMessageCount.put(senderId, 1);
            return false;
        } else {
            // اگر هنوز داخل همان 1 ثانیه باشد شمارنده افزایش یافته و بررسی می‌شود که از 5
            // بیشتر نباشد
            int count = userMessageCount.getOrDefault(senderId, 0) + 1;
            userMessageCount.put(senderId, count);
            return count > 5;
        }
    }
}