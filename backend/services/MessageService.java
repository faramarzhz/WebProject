package services;

import models.Chat;
import models.Group;
import models.Message;
import database.Database;
import java.util.HashMap;

public class MessageService {
    private HashMap<String, Long> userWindowStart = new HashMap<>();
    private HashMap<String, Integer> userMessageCount = new HashMap<>();

    public Message createMessage(String messageId, String senderId, String content, String mediaPath) {
        if (isSpam(senderId))
            return null;
        return new Message(messageId, senderId, content, mediaPath);
    }

    public void addMessageToChat(Chat chat, Message message) {
        if (chat != null && message != null) {
            chat.getMessages().add(message);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }

    public void addMessageToGroup(Group group, Message message) {
        if (group != null && message != null) {
            group.getMessages().add(message);
            Database.saveMessages(group.getGroupId(), group.getMessages());
        }
    }

    public void editMessage(Chat chat, Message message, String newContent) {
        if (message != null && !message.isDeleted()) {
            message.recordEditBeforeChange();
            message.setContent(newContent);
            message.setEdited(true);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }

    public void editMessageInGroup(Group group, Message message, String newContent) {
        if (message != null && !message.isDeleted()) {
            message.recordEditBeforeChange();
            message.setContent(newContent);
            message.setEdited(true);
            Database.saveMessages(group.getGroupId(), group.getMessages());
        }
    }

    public void deleteMessage(Chat chat, Message message) {
        if (message != null) {
            message.recordEditBeforeChange();
            message.setContent("This message was deleted.");
            message.setDeleted(true);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }

    public void deleteMessageInGroup(Group group, Message message) {
        if (message != null) {
            message.recordEditBeforeChange();
            message.setContent("This message was deleted.");
            message.setDeleted(true);
            Database.saveMessages(group.getGroupId(), group.getMessages());
        }
    }

    public void reportMessage(Chat chat, Message message) {
        if (message != null) {
            message.setReported(true);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }

    public void reactToMessage(Chat chat, Message message, String userId, String emoji) {
        if (message != null) {
            message.addReaction(userId, emoji);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }

    public void removeReaction(Chat chat, Message message, String userId) {
        if (message != null) {
            message.removeReaction(userId);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }

    private boolean isSpam(String senderId) {
        long now = System.currentTimeMillis();
        long windowStart = userWindowStart.getOrDefault(senderId, 0L);

        if (now - windowStart > 1000) {
            userWindowStart.put(senderId, now);
            userMessageCount.put(senderId, 1);
            return false;
        } else {
            int count = userMessageCount.getOrDefault(senderId, 0) + 1;
            userMessageCount.put(senderId, count);
            return count > 5;
        }
    }
}