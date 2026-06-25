package network;

import models.Chat;
import models.Group;
import models.Message;
import models.User;
import services.*;
import util.IdGenerator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RequestRouter {
    private Server server;
    // الگوهای رجکس برای استخراج داینامیک چت‌ ایدی از آدرس URL
    private static final Pattern MESSAGES_PATTERN = Pattern.compile("^/api/chat/([^/]+)/messages$");
    private static final Pattern SEND_PATTERN = Pattern.compile("^/api/chat/([^/]+)/send$");
    private static final Pattern REPORT_PATTERN = Pattern.compile("^/api/chat/([^/]+)/report$");

    public RequestRouter(Server server) {
        this.server = server;
    }

    // استخراج مقادیر رشته‌ای از درون JSON
    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1)
            return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    // استخراج مقادیر عددی از درون رشته JSON
    private String extractNumber(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start == -1)
            return "";
        start += key.length();
        while (start < json.length() && json.charAt(start) == ' ')
            start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-'))
            end++;
        return json.substring(start, end);
    }

    // مدیریت و هدایت درخواست‌ها به متدهای مربوطه بر اساس آدرس API و متد HTTP
    public String route(String method, String path, String queryString, String body) {
        // وب سرویس ثبت‌نام کاربر جدید
        if (method.equals("POST") && path.equals("/api/signup")) {
            String username = extractField(body, "username");
            String userId = extractField(body, "userId");
            String password = extractField(body, "password");
            String confirm = extractField(body, "confirmPassword");

            if (username.isEmpty() || userId.isEmpty() || password.isEmpty()) {
                return ResponseBuilder.error(400, "Missing required fields.");
            }
            if (!password.equals(confirm)) {
                return ResponseBuilder.error(400, "Passwords do not match.");
            }
            if (server.getUserService().userExists(userId)) {
                return ResponseBuilder.error(409, "User ID already taken. Please choose another.");
            }
            boolean success = server.getAuthService().register(username, password, userId);
            if (success) {
                return ResponseBuilder.created("{\"message\":\"Registered successfully\",\"userId\":\"" +
                        ResponseBuilder.escapeJson(userId) + "\"}");
            }
            return ResponseBuilder.error(400,
                    "Registration failed. Password must have uppercase, lowercase, digit and special char (!@#$%^&*).");
        }

        // وب سرویس ورود به حساب کاربر
        if (method.equals("POST") && path.equals("/api/login")) {
            String userId = extractField(body, "userId");
            String password = extractField(body, "password");

            if (userId.isEmpty() || password.isEmpty()) {
                return ResponseBuilder.error(400, "Missing userId or password.");
            }
            if (server.getAuthService().isBlocked(userId)) {
                return ResponseBuilder.error(429,
                        "Account temporarily locked due to too many failed attempts. Try again in 5 minutes.");
            }
            User user = server.getAuthService().login(userId, password);
            if (user != null) {
                return ResponseBuilder.ok("{\"message\":\"Login successful\",\"userId\":\"" +
                        ResponseBuilder.escapeJson(user.getUserId()) + "\",\"username\":\"" +
                        ResponseBuilder.escapeJson(user.getUsername()) + "\"}");
            }
            return ResponseBuilder.error(401, "Invalid User ID or password.");
        }

        // وب سرویس گرفتن کل لیست چت‌های یک کاربر مشخص
        if (method.equals("GET") && path.equals("/api/chats")) {
            String userId = getQueryParam(queryString, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.error(400, "Missing userId.");

            ArrayList<Chat> chats = server.getChatService().getChatsForUser(userId);
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Chat chat : chats) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append(chatToJson(chat, userId));
            }
            sb.append("]");
            return ResponseBuilder.ok(sb.toString());
        }

        // وب سرویس گرفتن یا ساخت پیام‌های ذخیره‌شده
        if (method.equals("GET") && path.equals("/api/chat/saved")) {
            String userId = getQueryParam(queryString, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.error(400, "Missing userId.");
            Chat saved = server.getChatService().getOrCreateSavedMessages(userId);
            return ResponseBuilder.ok("{\"chatId\":\"" + ResponseBuilder.escapeJson(saved.getChatId()) + "\"}");
        }

        // وب سرویس ایجاد چت خصوصی جدید بین دو یوزر
        if (method.equals("POST") && path.equals("/api/chat/create")) {
            String type = extractField(body, "type");
            String userId1 = extractField(body, "userId1");
            String userId2 = extractField(body, "userId2");

            if (userId1.isEmpty() || userId2.isEmpty()) {
                return ResponseBuilder.error(400, "Missing userId1 or userId2.");
            }
            if (!server.getUserService().userExists(userId2)) {
                return ResponseBuilder.error(404, "User '" + userId2 + "' not found.");
            }
            Chat existing = server.getChatService().findPrivateChat(userId1, userId2);
            if (existing != null) {
                return ResponseBuilder.ok("{\"chatId\":\"" +
                        ResponseBuilder.escapeJson(existing.getChatId()) + "\",\"existed\":true}");
            }
            String chatId = IdGenerator.generateId();
            Chat chat = server.getChatService().createChat(chatId, userId1, userId2);
            chat.setType("private");
            return ResponseBuilder.created("{\"chatId\":\"" +
                    ResponseBuilder.escapeJson(chat.getChatId()) + "\",\"existed\":false}");
        }

        // وب سرویس دریافت تاریخچه کامل پیام‌های یک چت
        if (method.equals("GET")) {
            Matcher matcher = MESSAGES_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.error(404, "Chat not found.");
                ArrayList<Message> msgs = chat.getMessages();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < msgs.size(); i++) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(messageToJson(msgs.get(i)));
                }
                sb.append("]");
                return ResponseBuilder.ok(sb.toString());
            }
        }

        // وب سرویس ارسال پیام جدید به یک چت
        if (method.equals("POST")) {
            Matcher matcher = SEND_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String sender = extractField(body, "senderId");
                String content = extractField(body, "content");

                if (sender.isEmpty() || content.isEmpty()) {
                    return ResponseBuilder.error(400, "Missing senderId or content.");
                }
                if (content.length() > 1000) {
                    return ResponseBuilder.error(400, "Message too long (max 1000 chars).");
                }
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.error(404, "Chat not found.");

                String msgId = IdGenerator.generateId();
                Message msg = server.getMessageService().createMessage(msgId, sender, content, null);
                if (msg == null) {
                    return ResponseBuilder.error(429, "Spam detected. Max 5 messages per second.");
                }
                server.getMessageService().addMessageToChat(chat, msg);
                return ResponseBuilder.ok("{\"messageId\":\"" + ResponseBuilder.escapeJson(msgId) + "\"}");
            }
        }

        // وب سرویس ریپورت
        if (method.equals("POST")) {
            Matcher matcher = REPORT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String reporter = extractField(body, "reporterId");
                String reason = extractField(body, "reason");

                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.error(404, "Chat not found.");

                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        server.getMessageService().reportMessage(m);
                        System.out.println("[REPORT] Chat:" + chatId + " Msg:" + msgId + " Reporter:" + reporter
                                + " Reason:" + reason);
                        return ResponseBuilder.ok("{\"message\":\"Message reported successfully.\"}");
                    }
                }
                return ResponseBuilder.error(404, "Message not found.");
            }
        }

        // وب سرویس حذف کامل اکانت یوزر
        if (method.equals("POST") && path.equals("/api/user/delete")) {
            String userId = extractField(body, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.error(400, "Missing userId.");
            if (!server.getUserService().userExists(userId)) {
                return ResponseBuilder.error(404, "User not found.");
            }
            server.getUserService().deleteAccount(userId);
            return ResponseBuilder.ok("{\"message\":\"Account deleted successfully.\"}");
        }

        // وب سرویس افزودن مخاطب جدید
        if (method.equals("POST") && path.equals("/api/contact/add")) {
            String userId = extractField(body, "userId");
            String contactId = extractField(body, "contactId");
            if (userId.isEmpty() || contactId.isEmpty())
                return ResponseBuilder.error(400, "Missing parameters.");
            server.getUserService().addContact(userId, contactId);
            return ResponseBuilder.ok("{\"message\":\"Contact added.\"}");
        }

        // وب سرویس ساخت گروه چت جدید
        if (method.equals("POST") && path.equals("/api/group/create")) {
            String groupName = extractField(body, "groupName");
            String creatorId = extractField(body, "creatorId");
            if (groupName.isEmpty() || creatorId.isEmpty())
                return ResponseBuilder.error(400, "Missing parameters.");
            String groupId = IdGenerator.generateId();
            Group g = server.getGroupService().createGroup(groupId, groupName, creatorId);
            return ResponseBuilder.created("{\"groupId\":\"" + ResponseBuilder.escapeJson(g.getGroupId())
                    + "\",\"groupName\":\"" + ResponseBuilder.escapeJson(g.getGroupName()) + "\"}");
        }
        return ResponseBuilder.error(404, "Endpoint not found: " + method + " " + path);
    }

    // تبدیل دستی داده‌های شی چت به ساختار متن JSON
    private String chatToJson(Chat chat, String currentUserId) {
        Message lastMsg = null;
        ArrayList<Message> msgs = chat.getMessages();
        if (!msgs.isEmpty())
            lastMsg = msgs.get(msgs.size() - 1);

        String lastContent = lastMsg != null ? ResponseBuilder.escapeJson(lastMsg.getContent()) : "";
        String lastTime = lastMsg != null ? String.valueOf(lastMsg.getTimestamp()) : "";

        int receivedMessages = 0;
        for (Message m : msgs) {
            if (!m.getSenderId().equals(currentUserId))
                receivedMessages++;
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(ResponseBuilder.escapeJson(chat.getChatId())).append("\",");
        sb.append("\"type\":\"").append(ResponseBuilder.escapeJson(chat.getType())).append("\",");
        sb.append("\"name\":\"").append(ResponseBuilder.escapeJson(chat.getName())).append("\",");
        sb.append("\"lastMessageContent\":\"").append(lastContent).append("\",");
        sb.append("\"lastMessageTime\":").append(lastTime.isEmpty() ? "null" : "\"" + lastTime + "\"").append(",");
        sb.append("\"totalMessages\":").append(receivedMessages).append(",");
        sb.append("\"participantIds\":[");
        ArrayList<String> parts = chat.getParticipants();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(ResponseBuilder.escapeJson(parts.get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    // تبدیل فیلدهای شی پیام به فرمت متنی JSON
    private String messageToJson(Message msg) {
        return "{" + "\"id\":\"" + ResponseBuilder.escapeJson(msg.getMessageId()) + "\"," + "\"senderId\":\""
                + ResponseBuilder.escapeJson(msg.getSenderId()) + "\"," + "\"content\":\""
                + ResponseBuilder.escapeJson(msg.getContent()) + "\"," + "\"time\":\"" + msg.getTimestamp() + "\","
                + "\"isEdited\":" + msg.isEdited() + "," + "\"isDeleted\":" + msg.isDeleted() + "," + "\"isReported\":"
                + msg.isReported() + "}";
    }

    // گرفتن و دیکود کردن متغیرها از داخل آدرس URL
    private String getQueryParam(String queryString, String key) {
        if (queryString == null || queryString.isEmpty())
            return "";
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try {
                    return java.net.URLDecoder.decode(kv[1], "UTF-8");
                } catch (Exception e) {
                    return kv[1];
                }
            }
        }
        return "";
    }
}