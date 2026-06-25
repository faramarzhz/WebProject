package models;

public class Message {
    private String messageId;
    private String senderId;
    private String content;
    private long timestamp;// زمان ارسال پیام
    private boolean isEdited;
    private boolean isDeleted;
    private String mediaPath;// برای فاز 2
    private boolean isReported;

    public Message(String messageId, String senderId, String content, String mediaPath) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        timestamp = System.currentTimeMillis();
        isEdited = false;
        isDeleted = false;
        this.mediaPath = mediaPath;
        isReported = false;
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
}