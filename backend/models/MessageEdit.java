package models;

public class MessageEdit {
    private String previousContent;
    private long editedAt;

    public MessageEdit(String previousContent) {
        this.previousContent = previousContent;
        editedAt = System.currentTimeMillis();
    }

    public String getPreviousContent() {
        return previousContent;
    }

    public long getEditedAt() {
        return editedAt;
    }
}