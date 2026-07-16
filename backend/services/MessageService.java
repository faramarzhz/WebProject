package services;

import models.Chat;
import models.Group;
import models.Message;
import database.Database;
import java.util.HashMap;

public class MessageService {
    private HashMap<String, Long> window = new HashMap<>();
    private HashMap<String, Integer> messageCount = new HashMap<>();

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

    private boolean isSpam(String senderId) {
        long now = System.currentTimeMillis();
        Long startVal = window.get(senderId);
        long start = (startVal != null) ? startVal : 0L;
        if (now - start > 1000) {
            window.put(senderId, now);
            messageCount.put(senderId, 1);
            return false;
        } 
        else {
            Integer countVal = messageCount.get(senderId);
            int count = ((countVal != null) ? countVal : 0) + 1;
            messageCount.put(senderId, count);
            return count > 5;
        }
    }
    
    public void addMessageToGroup(Group grop, Message message) {
        if (grop != null && message != null) {
            grop.getMessages().add(message);
            Database.saveMessages(grop.getGroupId(), grop.getMessages());
        }
    }

    public void editMessage(Chat chat, Message message, String newContent) {
        if (message != null && !message.isDeleted()) {
            message.oldtext();
            message.setContent(newContent);
            message.setEdited(true);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }
    public void editMessageInGroup(Group grop, Message message, String newContent) {
        if (message != null && !message.isDeleted()) {
            message.oldtext();
            message.setContent(newContent);
            message.setEdited(true);
            Database.saveMessages(grop.getGroupId(), grop.getMessages());
        }
    }
    public void deleteMessage(Chat chat, Message message) {
        if (message != null) {
            message.oldtext();
            message.setContent("This message was deleted.");
            message.setDeleted(true);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }
    public void deleteMessageInGroup(Group grop, Message message) {
        if (message != null) {
            message.oldtext();
            message.setContent("This message was deleted.");
            message.setDeleted(true);
            Database.saveMessages(grop.getGroupId(), grop.getMessages());
        }
    }

    public void reportMessage(Chat chat, Message message) {
        if (message != null) {
            message.setReported(true);
            Database.saveMessages(chat.getChatId(), chat.getMessages());
        }
    }
}