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
    private Pattern message = Pattern.compile("^/api/chat/([^/]+)/messages$");
    private Pattern send = Pattern.compile("^/api/chat/([^/]+)/send$");
    private Pattern REPORT_PATTERN = Pattern.compile("^/api/chat/([^/]+)/report$");
    private Pattern edit = Pattern.compile("^/api/chat/([^/]+)/message/edit$");
    private Pattern delete = Pattern.compile("^/api/chat/([^/]+)/message/delete$");
    private Pattern vakonesh = Pattern.compile("^/api/chat/([^/]+)/message/react$");
    private Pattern deletevako = Pattern.compile("^/api/chat/([^/]+)/message/unreact$");
    private Pattern history = Pattern.compile("^/api/chat/([^/]+)/message/history$");
    private Pattern groupsend = Pattern.compile("^/api/group/([^/]+)/send$");
    private Pattern groupmess = Pattern.compile("^/api/group/([^/]+)/messages$");
    private Pattern groupeditmess = Pattern.compile("^/api/group/([^/]+)/message/edit$");
    private Pattern groupdelmess = Pattern.compile("^/api/group/([^/]+)/message/delete$");
    public RequestRouter(Server server) {
        this.server = server;
    }

    private String extractField(String json, String fild) {
        String kelid = "\"" + fild + "\":\"";
        if (!json.contains(kelid))
            return "";
        String[] parts = json.split(kelid);
        String kelidbadi = parts[1];
        String[] pqrts = kelidbadi.split("\"");
        return pqrts[0];
    }

    public String route(String metod, String path, String query, String badane) {
        if (metod.equals("POST") && path.equals("/api/signup")) {
            String username = extractField(badane, "username");
            String userId = extractField(badane, "userId");
            String password = extractField(badane, "password");
            String confirm = extractField(badane, "confirmPassword");
            if (username.isEmpty() || userId.isEmpty() || password.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing required fields." + "\"}");
            if (!password.equals(confirm))
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Passwords do not match." + "\"}");
            if (server.getUserService().userExists(userId))
                return ResponseBuilder.buildResponse(409, "{\"error\":\"" + "User ID already taken. Please choose another." + "\"}");
            boolean success = server.getAuthService().register(username, password, userId);
            if (success)
                return ResponseBuilder.buildResponse(201, "{\"message\":\"Registered successfully\",\"userId\":\"" + userId + "\"}");
            return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Registration failed. Password must have uppercase, lowercase, digit and special char (!@#$%^&*)." + "\"}");
        }
        if (metod.equals("POST") && path.equals("/api/login")) {
            String userid = extractField(badane, "userId");
            String password = extractField(badane, "password");
            if (userid.isEmpty() || password.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or password." + "\"}");
            if (server.getAuthService().isBlocked(userid))
                return ResponseBuilder.buildResponse(429, "{\"error\":\"" + "Account temporarily locked due to too many failed attempts. Try again in 5 minutes." + "\"}");
            User user = server.getAuthService().login(userid, password);
            if (user != null)
                return ResponseBuilder.buildResponse(200, "{\"message\":\"Login successful\",\"userId\":\"" + user.getUserId() + "\",\"username\":\"" + user.getUsername() + "\"}");
            return ResponseBuilder.buildResponse(401, "{\"error\":\"" + "Invalid User ID or password." + "\"}");
        }
        if (metod.equals("GET") && path.equals("/api/chats")) {
            String userid = getQueryParam(query, "userId");
            if (userid.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            ArrayList<Chat> chats = server.getChatService().getChatsForUser(userid);
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Chat chat : chats) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append(chatToJson(chat, userid));
            }
            sb.append("]");
            return ResponseBuilder.buildResponse(200, sb.toString());
        }
        if (metod.equals("GET") && path.equals("/api/chat/saved")) {
            String userId = getQueryParam(query, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            Chat saved = server.getChatService().getOrCreateSavedMessages(userId);
            return ResponseBuilder.buildResponse(200, "{\"chatId\":\"" + saved.getChatId() + "\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/create")) {
            String type = extractField(badane, "type");
            String userId1 = extractField(badane, "userId1");
            String userId2 = extractField(badane, "userId2");
            if (userId1.isEmpty() || userId2.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId1 or userId2." + "\"}");
            if (!server.getUserService().userExists(userId2))
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "User '" + userId2 + "' not found." + "\"}");
            Chat existing = server.getChatService().findPrivateChat(userId1, userId2);
            if (existing != null)
                return ResponseBuilder.buildResponse(200, "{\"chatId\":\"" + existing.getChatId() + "\",\"existed\":true}");
            String chatId = IdGenerator.generateId();
            Chat chat = server.getChatService().createChat(chatId, userId1, userId2);
            chat.setType("private");
            return ResponseBuilder.buildResponse(201, "{\"chatId\":\"" + chat.getChatId() + "\",\"existed\":false}");
        }
        if (metod.equals("GET")) {
            Matcher matcher = message.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                ArrayList<Message> msgs = chat.getMessages();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < msgs.size(); i++) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(messageToJson(msgs.get(i)));
                }
                sb.append("]");
                return ResponseBuilder.buildResponse(200, sb.toString());
            }
        }
        if (metod.equals("GET")) {
            Matcher matcher = history.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = getQueryParam(query, "messageId");
                if (msgId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null) {
                    Group group = server.getGroups().get(chatId);
                    if (group == null)
                        return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                    for (Message m : group.getMessages()) {
                        if (m.getMessageId().equals(msgId))
                            return ResponseBuilder.buildResponse(200, messageHistoryToJson(m));
                    }
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
                }
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId))
                        return ResponseBuilder.buildResponse(200, messageHistoryToJson(m));
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = send.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String sender = extractField(badane, "senderId");
                String content = extractField(badane, "content");
                if (sender.isEmpty() || content.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing senderId or content." + "\"}");
                if (content.length() > 1000)
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Message too long (max 1000 chars)." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                String otherUserId = null;
                for (String p : chat.getUsers()) {
                    if (!p.equals(sender)) {
                        otherUserId = p;
                        break;
                    }
                }
                if (otherUserId != null) {
                    User otherUser = server.getUserService().getUserById(otherUserId);
                    if (otherUser != null && otherUser.hasBlocked(sender))
                        return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "You have been blocked by this user." + "\"}");
                }
                String msgId = IdGenerator.generateId();
                Message msg = server.getMessageService().createMessage(msgId, sender, content, null);
                if (msg == null)
                    return ResponseBuilder.buildResponse(429, "{\"error\":\"" + "Spam detected. Max 5 messages per second." + "\"}");
                server.getMessageService().addMessageToChat(chat, msg);
                if (otherUserId != null) {
                    WebSocketHandler receiverHandler = server.getActiveConnections().get(otherUserId);
                    if (receiverHandler != null)
                        receiverHandler.sendMessage(newMessageEventJson(msg));
                }
                return ResponseBuilder.buildResponse(200, "{\"messageId\":\"" + msgId + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = REPORT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String reporter = extractField(badane, "reporterId");
                String reason = extractField(badane, "reason");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        server.getMessageService().reportMessage(chat, m);
                        System.out.println("[REPORT] Chat:" + chatId + " Msg:" + msgId + " Reporter:" + reporter + " Reason:" + reason);
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Message reported successfully.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = edit.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String newContent = extractField(badane, "newContent");
                String senderId = extractField(badane, "senderId");
                if (msgId.isEmpty() || newContent.isEmpty() || senderId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId, newContent or senderId." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        if (!m.getSenderId().equals(senderId))
                            return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "You can only edit your own messages." + "\"}");
                        server.getMessageService().editMessage(chat, m, newContent);
                        for (String p : chat.getUsers()) {
                            if (!p.equals(senderId)) {
                                WebSocketHandler handler = server.getActiveConnections().get(p);
                                if (handler != null)
                                    handler.sendMessage(messageEditedEventJson(m));
                            }
                        }
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Message edited successfully.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = delete.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String senderId = extractField(badane, "senderId");
                if (msgId.isEmpty() || senderId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId or senderId." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        if (!m.getSenderId().equals(senderId))
                            return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "You can only delete your own messages." + "\"}");
                        server.getMessageService().deleteMessage(chat, m);
                        for (String p : chat.getUsers()) {
                            if (!p.equals(senderId)) {
                                WebSocketHandler handler = server.getActiveConnections().get(p);
                                if (handler != null)
                                    handler.sendMessage(messageDeletedEventJson(msgId));
                            }
                        }
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Message deleted successfully.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = vakonesh.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String userId = extractField(badane, "userId");
                String emoji = extractField(badane, "emoji");
                if (msgId.isEmpty() || userId.isEmpty() || emoji.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId, userId or emoji." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Reaction added.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = deletevako.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String userId = extractField(badane, "userId");
                if (msgId.isEmpty() || userId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId or userId." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Reaction removed.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("GET") && path.equals("/api/user/lastseen")) {
            String userId = getQueryParam(query, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            User user = server.getUserService().getUserById(userId);
            if (user == null)
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "User not found." + "\"}");
            boolean online = server.getActiveConnections().containsKey(userId);
            String json = "{\"online\":" + online + ",\"lastSeen\":" + user.getLastSeen() + "}";
            return ResponseBuilder.buildResponse(200, json);
        }
        if (metod.equals("POST") && path.equals("/api/user/delete")) {
            String userId = extractField(badane, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            if (!server.getUserService().userExists(userId))
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "User not found." + "\"}");
            server.getUserService().deleteAccount(userId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Account deleted successfully.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/user/block")) {
            String userId = extractField(badane, "userId");
            String targetId = extractField(badane, "targetId");
            if (userId.isEmpty() || targetId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or targetId." + "\"}");
            if (!server.getUserService().userExists(targetId))
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Target user not found." + "\"}");
            server.getUserService().blockUser(userId, targetId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"User blocked.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/user/unblock")) {
            String userId = extractField(badane, "userId");
            String targetId = extractField(badane, "targetId");
            if (userId.isEmpty() || targetId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or targetId." + "\"}");
            server.getUserService().unblockUser(userId, targetId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"User unblocked.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/contact/add")) {
            String userId = extractField(badane, "userId");
            String contactId = extractField(badane, "contactId");
            if (userId.isEmpty() || contactId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            server.getUserService().addContact(userId, contactId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Contact added.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/contact/remove")) {
            String userId = extractField(badane, "userId");
            String contactId = extractField(badane, "contactId");
            if (userId.isEmpty() || contactId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            server.getUserService().removeContact(userId, contactId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Contact removed.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/pin")) {
            String userId = extractField(badane, "userId");
            String chatId = extractField(badane, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().pinChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat pinned.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/unpin")) {
            String userId = extractField(badane, "userId");
            String chatId = extractField(badane, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().unpinChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat unpinned.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/archive")) {
            String userId = extractField(badane, "userId");
            String chatId = extractField(badane, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().archiveChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat archived.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/unarchive")) {
            String userId = extractField(badane, "userId");
            String chatId = extractField(badane, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().unarchiveChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat unarchived.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/mute")) {
            String userId = extractField(badane, "userId");
            String chatId = extractField(badane, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().muteChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat muted.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/chat/unmute")) {
            String userId = extractField(badane, "userId");
            String chatId = extractField(badane, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().unmuteChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat unmuted.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/group/create")) {
            String groupName = extractField(badane, "groupName");
            String creatorId = extractField(badane, "creatorId");
            if (groupName.isEmpty() || creatorId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            String groupId = IdGenerator.generateId();
            Group g = server.getGroupService().createGroup(groupId, groupName, creatorId);
            return ResponseBuilder.buildResponse(201, "{\"groupId\":\"" + g.getGroupId() + "\",\"groupName\":\"" + g.getName() + "\"}");
        }
        if (metod.equals("GET") && path.equals("/api/groups")) {
            String userId = getQueryParam(query, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            ArrayList<Group> groups = server.getGroupService().getGroupsForUser(userId);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < groups.size(); i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(groupToJson(groups.get(i), userId));
            }
            sb.append("]");
            return ResponseBuilder.buildResponse(200, sb.toString());
        }
        if (metod.equals("POST")) {
            Matcher matcher = groupsend.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                String sender = extractField(badane, "senderId");
                String content = extractField(badane, "content");
                if (sender.isEmpty() || content.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing senderId or content." + "\"}");
                if (content.length() > 1000)
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Message too long (max 1000 chars)." + "\"}");
                Group group = server.getGroupService().getGroupById(groupId);
                if (group == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
                if (!group.getMembers().contains(sender))
                    return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "You are not a member of this group." + "\"}");
                String msgId = IdGenerator.generateId();
                Message msg = server.getMessageService().createMessage(msgId, sender, content, null);
                if (msg == null)
                    return ResponseBuilder.buildResponse(429, "{\"error\":\"" + "Spam detected. Max 5 messages per second." + "\"}");
                server.getMessageService().addMessageToGroup(group, msg);
                for (String memberId : group.getMembers()) {
                    if (!memberId.equals(sender)) {
                        WebSocketHandler memberHandler = server.getActiveConnections().get(memberId);
                        if (memberHandler != null)
                            memberHandler.sendMessage(newMessageEventJson(msg));
                    }
                }
                return ResponseBuilder.buildResponse(200, "{\"messageId\":\"" + msgId + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = groupeditmess.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String newContent = extractField(badane, "newContent");
                String senderId = extractField(badane, "senderId");
                if (msgId.isEmpty() || newContent.isEmpty() || senderId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId, newContent or senderId." + "\"}");
                Group group = server.getGroupService().getGroupById(groupId);
                if (group == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
                for (Message m : group.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        if (!m.getSenderId().equals(senderId))
                            return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "You can only edit your own messages." + "\"}");
                        server.getMessageService().editMessageInGroup(group, m, newContent);
                        for (String memberId : group.getMembers()) {
                            if (!memberId.equals(senderId)) {
                                WebSocketHandler handler = server.getActiveConnections().get(memberId);
                                if (handler != null)
                                    handler.sendMessage(messageEditedEventJson(m));
                            }
                        }
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Message edited successfully.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("POST")) {
            Matcher matcher = groupdelmess.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                String msgId = extractField(badane, "messageId");
                String senderId = extractField(badane, "senderId");
                if (msgId.isEmpty() || senderId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId or senderId." + "\"}");
                Group group = server.getGroupService().getGroupById(groupId);
                if (group == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
                for (Message m : group.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        if (!m.getSenderId().equals(senderId))
                            return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "You can only delete your own messages." + "\"}");
                        server.getMessageService().deleteMessageInGroup(group, m);
                        for (String memberId : group.getMembers()) {
                            if (!memberId.equals(senderId)) {
                                WebSocketHandler handler = server.getActiveConnections().get(memberId);
                                if (handler != null)
                                    handler.sendMessage(messageDeletedEventJson(msgId));
                            }
                        }
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Message deleted successfully.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (metod.equals("GET")) {
            Matcher matcher = groupmess.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                Group group = server.getGroupService().getGroupById(groupId);
                if (group == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
                ArrayList<Message> msgs = group.getMessages();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < msgs.size(); i++) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(messageToJson(msgs.get(i)));
                }
                sb.append("]");
                return ResponseBuilder.buildResponse(200, sb.toString());
            }
        }
        if (metod.equals("POST") && path.equals("/api/group/addmember")) {
            String groupId = extractField(badane, "groupId");
            String requesterId = extractField(badane, "requesterId");
            String userId = extractField(badane, "userId");
            if (groupId.isEmpty() || requesterId.isEmpty() || userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            Group group = server.getGroupService().getGroupById(groupId);
            if (group == null)
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
            if (!group.isAdmin(requesterId))
                return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "Only group admins can add members." + "\"}");
            if (!server.getUserService().userExists(userId))
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "User not found." + "\"}");
            server.getGroupService().addMember(groupId, userId);
            for (String memberId : group.getMembers()) {
                WebSocketHandler handler = server.getActiveConnections().get(memberId);
                if (handler != null)
                    handler.sendMessage(memberAddedEventJson(groupId, userId));
            }
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Member added.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/group/removemember")) {
            String groupId = extractField(badane, "groupId");
            String requesterId = extractField(badane, "requesterId");
            String userId = extractField(badane, "userId");
            if (groupId.isEmpty() || requesterId.isEmpty() || userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            Group group = server.getGroupService().getGroupById(groupId);
            if (group == null)
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
            boolean isSelfLeaving = requesterId.equals(userId);
            if (!isSelfLeaving && !group.isAdmin(requesterId))
                return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "Only group admins can remove other members." + "\"}");
            WebSocketHandler removedUserHandler = server.getActiveConnections().get(userId);
            if (removedUserHandler != null)
                removedUserHandler.sendMessage(memberRemovedEventJson(groupId, userId));
            server.getGroupService().removeMember(groupId, userId);
            for (String memberId : group.getMembers()) {
                WebSocketHandler handler = server.getActiveConnections().get(memberId);
                if (handler != null)
                    handler.sendMessage(memberRemovedEventJson(groupId, userId));
            }
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Member removed.\"}");
        }
        if (metod.equals("POST") && path.equals("/api/group/update")) {
            String groupId = extractField(badane, "groupId");
            String requesterId = extractField(badane, "requesterId");
            String newGroupName = extractField(badane, "groupName");
            String newPic = extractField(badane, "profilePicturePath");
            if (groupId.isEmpty() || requesterId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing groupId or requesterId." + "\"}");
            Group group = server.getGroupService().getGroupById(groupId);
            if (group == null)
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Group not found." + "\"}");
            if (!group.isAdmin(requesterId))
                return ResponseBuilder.buildResponse(403, "{\"error\":\"" + "Only group admins can update group info." + "\"}");
            server.getGroupService().updateGroupInfo(groupId, newGroupName, newPic);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Group info updated.\"}");
        }
        return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Endpoint not found: " + metod + " " + path + "\"}");
    }

    private String chatToJson(Chat chat, String currentUserId) {
        Message lastMsg = null;
        ArrayList<Message> msgs = chat.getMessages();
        if (!msgs.isEmpty())
            lastMsg = msgs.get(msgs.size() - 1);
        String lastContent = lastMsg != null ? lastMsg.getContent() : "";
        String lastTime = lastMsg != null ? String.valueOf(lastMsg.getTimestamp()) : "";
        int receivedMessages = 0;
        for (Message m : msgs) {
            if (!m.getSenderId().equals(currentUserId))
                receivedMessages++;
        }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(chat.getChatId()).append("\",");
        sb.append("\"type\":\"").append(chat.getType()).append("\",");
        sb.append("\"name\":\"").append(chat.getName()).append("\",");
        sb.append("\"lastMessageContent\":\"").append(lastContent).append("\",");
        sb.append("\"lastMessageTime\":").append(lastTime.isEmpty() ? "null" : "\"" + lastTime + "\"").append(",");
        sb.append("\"totalMessages\":").append(receivedMessages).append(",");
        User currentUser = server.getUserService().getUserById(currentUserId);
        boolean isPinned = currentUser != null && currentUser.getPinChat().contains(chat.getChatId());
        boolean isArchived = currentUser != null && currentUser.getArchiveChat().contains(chat.getChatId());
        boolean isMuted = currentUser != null && currentUser.getMuteChat().contains(chat.getChatId());
        sb.append("\"isPinned\":").append(isPinned).append(",");
        sb.append("\"isArchived\":").append(isArchived).append(",");
        sb.append("\"isMuted\":").append(isMuted).append(",");
        sb.append("\"participantIds\":[");
        ArrayList<String> parts = chat.getUsers();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(parts.get(i)).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }
    private String groupToJson(Group group, String currentUserId) {
        Message lastMsg = null;
        ArrayList<Message> msgs = group.getMessages();
        if (!msgs.isEmpty())
            lastMsg = msgs.get(msgs.size() - 1);
        String lastContent = lastMsg != null ? lastMsg.getContent() : "";
        String lastTime = lastMsg != null ? String.valueOf(lastMsg.getTimestamp()) : "";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(group.getGroupId()).append("\",");
        sb.append("\"name\":\"").append(group.getName()).append("\",");
        sb.append("\"lastMessageContent\":\"").append(lastContent).append("\",");
        sb.append("\"lastMessageTime\":").append(lastTime.isEmpty() ? "null" : "\"" + lastTime + "\"").append(",");
        sb.append("\"lastMessageTime\":").append(lastTime.isEmpty() ? "null" : "\"" + lastTime + "\"").append(",");
        User currentUser = server.getUserService().getUserById(currentUserId);
        boolean isPinned = currentUser != null && currentUser.getPinChat().contains(group.getGroupId());
        boolean isArchived = currentUser != null && currentUser.getArchiveChat().contains(group.getGroupId());
        boolean isMuted = currentUser != null && currentUser.getMuteChat().contains(group.getGroupId());
        sb.append("\"isPinned\":").append(isPinned).append(",");
        sb.append("\"isArchived\":").append(isArchived).append(",");
        sb.append("\"isMuted\":").append(isMuted).append(",");
        sb.append("\"memberIds\":[");
        int i = 0;
        for (String memberId : group.getMembers()) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(memberId).append("\"");
            i++;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String messageHistoryToJson(Message msg) {
        StringBuilder sb = new StringBuilder("{\"messageId\":\"" + msg.getMessageId() + "\",");
        sb.append("\"isDeleted\":").append(msg.isDeleted()).append(",");
        sb.append("\"isEdited\":").append(msg.isEdited()).append(",");
        sb.append("\"currentContent\":\"").append(msg.getContent()).append("\",");
        sb.append("\"history\":[");
        ArrayList<models.MessageEdit> edits = msg.getEditHistory();
        for (int i = 0; i < edits.size(); i++) {
            if (i > 0)
                sb.append(",");
            models.MessageEdit edit = edits.get(i);
            sb.append("{\"previousContent\":\"").append(edit.getPreviousContent()).append("\",");
            sb.append("\"editedAt\":").append(edit.getEditedAt()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String messageToJson(Message msg) {
        return "{" + "\"id\":\"" + msg.getMessageId() + "\"," + "\"senderId\":\"" + msg.getSenderId() + "\"," + "\"content\":\"" + msg.getContent() + "\"," + "\"time\":\"" + msg.getTimestamp() + "\"," + "\"isEdited\":" + msg.isEdited() + "," + "\"isDeleted\":" + msg.isDeleted() + "," + "\"isReported\":" + msg.isReported() + "}";
    }

    private String newMessageEventJson(Message msg) {
        return "{\"type\":\"newMessage\",\"message\":" + messageToJson(msg) + "}";
    }

    private String messageEditedEventJson(Message msg) {
        return "{\"type\":\"messageEdited\",\"message\":" + messageToJson(msg) + "}";
    }

    private String messageDeletedEventJson(String messageId) {
        return "{\"type\":\"messageDeleted\",\"messageId\":\"" + messageId + "\"}";
    }

    private String memberAddedEventJson(String groupId, String userId) {
        return "{\"type\":\"memberAdded\",\"groupId\":\"" + groupId + "\",\"userId\":\"" + userId + "\"}";
    }

    private String memberRemovedEventJson(String groupId, String userId) {
        return "{\"type\":\"memberRemoved\",\"groupId\":\"" + groupId + "\",\"userId\":\"" + userId + "\"}";
    }

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