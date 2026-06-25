package services;

import models.User;
import java.util.HashMap;

public class UserService {
    private HashMap<String, User> users; // ذخیره کل کاربران سرور با هش مپ برای دسترسی سریع با userId

    public UserService(HashMap<String, User> users) {
        this.users = users;
    }

    // گرفتن یک یوزر خاص بر اساس آیدی آن
    public User getUserById(String userId) {
        return users.get(userId);
    }

    // چک کردن وجود داشتن یک یوزر در سرور
    public boolean userExists(String userId) {
        return users.containsKey(userId);
    }

    // متد تغییر آیدی کاربر با جابجایی کلید در هش مپ
    public boolean changeUserId(String oldId, String newId) {
        if (users.containsKey(newId))
            return false; // اگر آیدی جدید از قبل وجود داشت لغو می‌شود
        User user = users.remove(oldId); // حذف یوزر با آیدی قدیمی از مپ و گرفتن شی آن
        if (user == null)
            return false; // اگر یوزری با آیدی قدیمی پیدا نشد
        user.setUserId(newId); // ست کردن آیدی جدید روی خود شی یوزر
        users.put(newId, user); // قرار دادن مجدد یوزر درمپ با آیدی جدید
        return true;
    }

    // آپدیت نام کاربری و عکس پروفایل یوزر
    public void updateProfile(String userId, String newUsername, String newProfilePicturePath) {
        User user = users.get(userId);
        if (user != null) {
            if (newUsername != null && !newUsername.isEmpty())
                user.setUsername(newUsername);
            if (newProfilePicturePath != null)
                user.setProfilePicturePath(newProfilePicturePath);
        }
    }

    // حذف کامل اکانت یوزر از سرور
    public void deleteAccount(String userId) {
        users.remove(userId);
    }

    // اضافه کردن آیدی یوزر به لیست مخاطبین
    public void addContact(String userId, String contactId) {
        User user = users.get(userId);
        if (user != null && users.containsKey(contactId))
            user.getContacts().add(contactId);
    }
}