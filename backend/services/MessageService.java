package services;

import models.Chat;
import models.Group;
import models.Message;
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
        if (chat != null && message != null)
            chat.getMessages().add(message);
    }

    public void addMessageToGroup(Group group, Message message) {
        if (group != null && message != null)
            group.getMessages().add(message);
    }

    public void editMessage(Message message, String newContent) {
        if (message != null && !message.isDeleted()) {
            message.recordEditBeforeChange();
            message.setContent(newContent);
            message.setEdited(true);
        }
    }

    public void deleteMessage(Message message) {
        if (message != null) {
            message.recordEditBeforeChange();
            message.setContent("This message was deleted.");
            message.setDeleted(true);
        }
    }

    public void reportMessage(Message message) {
        if (message != null)
            message.setReported(true);
    }

    public void reactToMessage(Message message, String userId, String emoji) {
        if (message != null)
            message.addReaction(userId, emoji);
    }

    public void removeReaction(Message message, String userId) {
        if (message != null)
            message.removeReaction(userId);
    }

    private boolean isSpam(String senderId) {
        long now = System.currentTimeMillis();
        long windowStart = 0;

        if (userWindowStart.containsKey(senderId))
            windowStart = userWindowStart.get(senderId);
        if (now - windowStart > 1000) {
            userWindowStart.put(senderId, now);
            userMessageCount.put(senderId, 1);
            return false;
        } else {
            int count = 1;
            if (userMessageCount.containsKey(senderId))
                count = userMessageCount.get(senderId) + 1;
            userMessageCount.put(senderId, count);
            return count > 5;
        }
    }
}