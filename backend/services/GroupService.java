package services;

import models.Group;
import java.util.ArrayList;
import java.util.HashMap;

public class GroupService {
    private HashMap<String, Group> groups = new HashMap<>(); // ذخیره کل گروه‌های سرور با هش مپ برای دسترسی سریع با
                                                             // groupId

    // متد ساخت گروه جدید و عضو کردن سازنده
    public Group createGroup(String groupId, String groupName, String creatorId) {
        Group newGroup = new Group(groupId, groupName);
        newGroup.getMembers().add(creatorId); // سازنده گروه به عنوان اولین عضو اضافه می‌شود
        groups.put(groupId, newGroup);
        return newGroup;
    }

    // اضافه کردن ممبر جدید به گروه
    public void addMember(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group != null)
            group.getMembers().add(userId);
    }

    // حذف ممبر از گروه (لفت دادن یا ریمو شدن)
    public void removeMember(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group != null)
            group.getMembers().remove(userId);
    }

    // آپدیت نام و عکس پروفایل گروه
    public void updateGroupInfo(String groupId, String newGroupName, String newPic) {
        Group group = groups.get(groupId);
        if (group != null) {
            if (newGroupName != null && !newGroupName.isEmpty())
                group.setGroupName(newGroupName);
            if (newPic != null)
                group.setProfilePicturePath(newPic);
        }
    }

    // گرفتن یک گروه خاص بر اساس آیدی آن
    public Group getGroupById(String groupId) {
        return groups.get(groupId);
    }

    // دسترسی مستقیم به کل مپ گروه‌ها
    public HashMap<String, Group> getAllGroups() {
        return groups;
    }

    // فیلتر کردن و استخراج تمام گروه‌هایی که یک یوزر مشخص در آن‌ها عضویت دارد
    public ArrayList<Group> getGroupsForUser(String userId) {
        ArrayList<Group> result = new ArrayList<>();
        for (Group g : groups.values()) {
            if (g.getMembers().contains(userId))
                result.add(g);
        }
        return result;
    }
}