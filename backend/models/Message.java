package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Message {
    private String messageId;
    private String senderId;
    private String content;
    private long timestamp;// زمان ارسال پیام
    private boolean isEdited;
    private boolean isDeleted;
    private String mediaPath;
    private boolean isReported;

    private ArrayList<MessageEdit> editHistory; // تاریخچه‌ی نسخه‌های قبلی پیام (قبل از هر ویرایش یا حذف)
    private LinkedHashMap<String, String> reactions; // نگاشت userId به ایموجی ری‌اکشن LinkedHashMap برای حفظ ترتیب ثبت

    public Message(String messageId, String senderId, String content, String mediaPath) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        timestamp = System.currentTimeMillis();
        isEdited = false;
        isDeleted = false;
        this.mediaPath = mediaPath;
        isReported = false;
        editHistory = new ArrayList<>();
        reactions = new LinkedHashMap<>();
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public boolean isReported() {
        return isReported;
    }

    public void setReported(boolean isReported) {
        this.isReported = isReported;
    }

    public void recordEditBeforeChange() {
        editHistory.add(new MessageEdit(this.content));
    }

    public ArrayList<MessageEdit> getEditHistory() {
        return editHistory;
    }

    public void addReaction(String userId, String emoji) {
        reactions.put(userId, emoji);
    } 

    public void removeReaction(String userId) {
        reactions.remove(userId);
    }

    public LinkedHashMap<String, String> getReactions() {
        return reactions;
    }
}