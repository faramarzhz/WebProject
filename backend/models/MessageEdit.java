package models;

// این کلاس نمای یک پیام را در لحظه‌ی قبل از ویرایش یا حذف نگه می‌دارد
// هر بار که محتوای پیام تغییر کند (ویرایش) یا حذف شود، یک نمونه از این کلاس ساخته می‌شود
// و به تاریخچه‌ی همان پیام اضافه می‌گردد تا در بخش "تاریخچه" قابل مشاهده باشد
public class MessageEdit {
    private String previousContent; // محتوای پیام قبل از این تغییر
    private long editedAt; // زمانی که این تغییر رخ داده است

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
