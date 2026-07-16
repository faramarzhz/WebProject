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
    private Pattern MESSAGES_PATTERN = Pattern.compile("^/api/chat/([^/]+)/messages$");
    private Pattern SEND_PATTERN = Pattern.compile("^/api/chat/([^/]+)/send$");
    private Pattern REPORT_PATTERN = Pattern.compile("^/api/chat/([^/]+)/report$");
    private Pattern MESSAGE_EDIT_PATTERN = Pattern.compile("^/api/chat/([^/]+)/message/edit$");
    private Pattern MESSAGE_DELETE_PATTERN = Pattern.compile("^/api/chat/([^/]+)/message/delete$");
    private Pattern MESSAGE_REACT_PATTERN = Pattern.compile("^/api/chat/([^/]+)/message/react$");
    private Pattern MESSAGE_UNREACT_PATTERN = Pattern.compile("^/api/chat/([^/]+)/message/unreact$");
    private Pattern MESSAGE_HISTORY_PATTERN = Pattern.compile("^/api/chat/([^/]+)/message/history$");
    private Pattern GROUP_SEND_PATTERN = Pattern.compile("^/api/group/([^/]+)/send$");
    private Pattern GROUP_MESSAGES_PATTERN = Pattern.compile("^/api/group/([^/]+)/messages$");
    private Pattern GROUP_MESSAGE_EDIT_PATTERN = Pattern.compile("^/api/group/([^/]+)/message/edit$");
    private Pattern GROUP_MESSAGE_DELETE_PATTERN = Pattern.compile("^/api/group/([^/]+)/message/delete$");
    public RequestRouter(Server server) {
        this.server = server;
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        if (!json.contains(key))
            return "";
        String[] parts = json.split(key);
        String afterKey = parts[1];
        String[] valueParts = afterKey.split("\"");
        return valueParts[0];
    }

    public String route(String method, String path, String queryString, String body) {
        if (method.equals("POST") && path.equals("/api/signup")) {
            String username = extractField(body, "username");
            String userId = extractField(body, "userId");
            String password = extractField(body, "password");
            String confirm = extractField(body, "confirmPassword");
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
        if (method.equals("POST") && path.equals("/api/login")) {
            String userId = extractField(body, "userId");
            String password = extractField(body, "password");
            if (userId.isEmpty() || password.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or password." + "\"}");
            if (server.getAuthService().isBlocked(userId))
                return ResponseBuilder.buildResponse(429, "{\"error\":\"" + "Account temporarily locked due to too many failed attempts. Try again in 5 minutes." + "\"}");
            User user = server.getAuthService().login(userId, password);
            if (user != null)
                return ResponseBuilder.buildResponse(200, "{\"message\":\"Login successful\",\"userId\":\"" + user.getUserId() + "\",\"username\":\"" + user.getUsername() + "\"}");
            return ResponseBuilder.buildResponse(401, "{\"error\":\"" + "Invalid User ID or password." + "\"}");
        }
        if (method.equals("GET") && path.equals("/api/chats")) {
            String userId = getQueryParam(queryString, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
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
            return ResponseBuilder.buildResponse(200, sb.toString());
        }
        if (method.equals("GET") && path.equals("/api/chat/saved")) {
            String userId = getQueryParam(queryString, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            Chat saved = server.getChatService().getOrCreateSavedMessages(userId);
            return ResponseBuilder.buildResponse(200, "{\"chatId\":\"" + saved.getChatId() + "\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/create")) {
            String type = extractField(body, "type");
            String userId1 = extractField(body, "userId1");
            String userId2 = extractField(body, "userId2");
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
        if (method.equals("GET")) {
            Matcher matcher = MESSAGES_PATTERN.matcher(path);
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
        if (method.equals("GET")) {
            Matcher matcher = MESSAGE_HISTORY_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = getQueryParam(queryString, "messageId");
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
        if (method.equals("POST")) {
            Matcher matcher = SEND_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String sender = extractField(body, "senderId");
                String content = extractField(body, "content");
                if (sender.isEmpty() || content.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing senderId or content." + "\"}");
                if (content.length() > 1000)
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Message too long (max 1000 chars)." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                String otherUserId = null;
                for (String p : chat.getParticipants()) {
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
        if (method.equals("POST")) {
            Matcher matcher = REPORT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String reporter = extractField(body, "reporterId");
                String reason = extractField(body, "reason");
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
        if (method.equals("POST")) {
            Matcher matcher = MESSAGE_EDIT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String newContent = extractField(body, "newContent");
                String senderId = extractField(body, "senderId");
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
                        for (String p : chat.getParticipants()) {
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
        if (method.equals("POST")) {
            Matcher matcher = MESSAGE_DELETE_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String senderId = extractField(body, "senderId");
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
                        for (String p : chat.getParticipants()) {
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
        if (method.equals("POST")) {
            Matcher matcher = MESSAGE_REACT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String userId = extractField(body, "userId");
                String emoji = extractField(body, "emoji");
                if (msgId.isEmpty() || userId.isEmpty() || emoji.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId, userId or emoji." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        server.getMessageService().reactToMessage(chat, m, userId, emoji);
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Reaction added.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (method.equals("POST")) {
            Matcher matcher = MESSAGE_UNREACT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String chatId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String userId = extractField(body, "userId");
                if (msgId.isEmpty() || userId.isEmpty())
                    return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing messageId or userId." + "\"}");
                Chat chat = server.getChatService().getChatById(chatId);
                if (chat == null)
                    return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Chat not found." + "\"}");
                for (Message m : chat.getMessages()) {
                    if (m.getMessageId().equals(msgId)) {
                        server.getMessageService().removeReaction(chat, m, userId);
                        return ResponseBuilder.buildResponse(200, "{\"message\":\"Reaction removed.\"}");
                    }
                }
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Message not found." + "\"}");
            }
        }
        if (method.equals("GET") && path.equals("/api/user/lastseen")) {
            String userId = getQueryParam(queryString, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            User user = server.getUserService().getUserById(userId);
            if (user == null)
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "User not found." + "\"}");
            boolean online = server.getActiveConnections().containsKey(userId);
            String json = "{\"online\":" + online + ",\"lastSeen\":" + user.getLastSeen() + "}";
            return ResponseBuilder.buildResponse(200, json);
        }
        if (method.equals("POST") && path.equals("/api/user/delete")) {
            String userId = extractField(body, "userId");
            if (userId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId." + "\"}");
            if (!server.getUserService().userExists(userId))
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "User not found." + "\"}");
            server.getUserService().deleteAccount(userId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Account deleted successfully.\"}");
        }
        if (method.equals("POST") && path.equals("/api/user/block")) {
            String userId = extractField(body, "userId");
            String targetId = extractField(body, "targetId");
            if (userId.isEmpty() || targetId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or targetId." + "\"}");
            if (!server.getUserService().userExists(targetId))
                return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Target user not found." + "\"}");
            server.getUserService().blockUser(userId, targetId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"User blocked.\"}");
        }
        if (method.equals("POST") && path.equals("/api/user/unblock")) {
            String userId = extractField(body, "userId");
            String targetId = extractField(body, "targetId");
            if (userId.isEmpty() || targetId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or targetId." + "\"}");
            server.getUserService().unblockUser(userId, targetId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"User unblocked.\"}");
        }
        if (method.equals("POST") && path.equals("/api/contact/add")) {
            String userId = extractField(body, "userId");
            String contactId = extractField(body, "contactId");
            if (userId.isEmpty() || contactId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            server.getUserService().addContact(userId, contactId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Contact added.\"}");
        }
        if (method.equals("POST") && path.equals("/api/contact/remove")) {
            String userId = extractField(body, "userId");
            String contactId = extractField(body, "contactId");
            if (userId.isEmpty() || contactId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            server.getUserService().removeContact(userId, contactId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Contact removed.\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/pin")) {
            String userId = extractField(body, "userId");
            String chatId = extractField(body, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().pinChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat pinned.\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/unpin")) {
            String userId = extractField(body, "userId");
            String chatId = extractField(body, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().unpinChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat unpinned.\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/archive")) {
            String userId = extractField(body, "userId");
            String chatId = extractField(body, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().archiveChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat archived.\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/unarchive")) {
            String userId = extractField(body, "userId");
            String chatId = extractField(body, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().unarchiveChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat unarchived.\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/mute")) {
            String userId = extractField(body, "userId");
            String chatId = extractField(body, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().muteChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat muted.\"}");
        }
        if (method.equals("POST") && path.equals("/api/chat/unmute")) {
            String userId = extractField(body, "userId");
            String chatId = extractField(body, "chatId");
            if (userId.isEmpty() || chatId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing userId or chatId." + "\"}");
            server.getUserService().unmuteChat(userId, chatId);
            return ResponseBuilder.buildResponse(200, "{\"message\":\"Chat unmuted.\"}");
        }
        if (method.equals("POST") && path.equals("/api/group/create")) {
            String groupName = extractField(body, "groupName");
            String creatorId = extractField(body, "creatorId");
            if (groupName.isEmpty() || creatorId.isEmpty())
                return ResponseBuilder.buildResponse(400, "{\"error\":\"" + "Missing parameters." + "\"}");
            String groupId = IdGenerator.generateId();
            Group g = server.getGroupService().createGroup(groupId, groupName, creatorId);
            return ResponseBuilder.buildResponse(201, "{\"groupId\":\"" + g.getGroupId() + "\",\"groupName\":\"" + g.getGroupName() + "\"}");
        }
        if (method.equals("GET") && path.equals("/api/groups")) {
            String userId = getQueryParam(queryString, "userId");
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
        if (method.equals("POST")) {
            Matcher matcher = GROUP_SEND_PATTERN.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                String sender = extractField(body, "senderId");
                String content = extractField(body, "content");
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
        if (method.equals("POST")) {
            Matcher matcher = GROUP_MESSAGE_EDIT_PATTERN.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String newContent = extractField(body, "newContent");
                String senderId = extractField(body, "senderId");
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
        if (method.equals("POST")) {
            Matcher matcher = GROUP_MESSAGE_DELETE_PATTERN.matcher(path);
            if (matcher.matches()) {
                String groupId = matcher.group(1);
                String msgId = extractField(body, "messageId");
                String senderId = extractField(body, "senderId");
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
        if (method.equals("GET")) {
            Matcher matcher = GROUP_MESSAGES_PATTERN.matcher(path);
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
        if (method.equals("POST") && path.equals("/api/group/addmember")) {
            String groupId = extractField(body, "groupId");
            String requesterId = extractField(body, "requesterId");
            String userId = extractField(body, "userId");
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
        if (method.equals("POST") && path.equals("/api/group/removemember")) {
            String groupId = extractField(body, "groupId");
            String requesterId = extractField(body, "requesterId");
            String userId = extractField(body, "userId");
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
        if (method.equals("POST") && path.equals("/api/group/update")) {
            String groupId = extractField(body, "groupId");
            String requesterId = extractField(body, "requesterId");
            String newGroupName = extractField(body, "groupName");
            String newPic = extractField(body, "profilePicturePath");
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
        return ResponseBuilder.buildResponse(404, "{\"error\":\"" + "Endpoint not found: " + method + " " + path + "\"}");
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
        boolean isPinned = currentUser != null && currentUser.getPinnedChatIds().contains(chat.getChatId());
        boolean isArchived = currentUser != null && currentUser.getArchivedChatIds().contains(chat.getChatId());
        boolean isMuted = currentUser != null && currentUser.getMutedChatIds().contains(chat.getChatId());
        sb.append("\"isPinned\":").append(isPinned).append(",");
        sb.append("\"isArchived\":").append(isArchived).append(",");
        sb.append("\"isMuted\":").append(isMuted).append(",");
        sb.append("\"participantIds\":[");
        ArrayList<String> parts = chat.getParticipants();
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
        sb.append("\"name\":\"").append(group.getGroupName()).append("\",");
        sb.append("\"lastMessageContent\":\"").append(lastContent).append("\",");
        sb.append("\"lastMessageTime\":").append(lastTime.isEmpty() ? "null" : "\"" + lastTime + "\"").append(",");
        sb.append("\"lastMessageTime\":").append(lastTime.isEmpty() ? "null" : "\"" + lastTime + "\"").append(",");
        User currentUser = server.getUserService().getUserById(currentUserId);
        boolean isPinned = currentUser != null && currentUser.getPinnedChatIds().contains(group.getGroupId());
        boolean isArchived = currentUser != null && currentUser.getArchivedChatIds().contains(group.getGroupId());
        boolean isMuted = currentUser != null && currentUser.getMutedChatIds().contains(group.getGroupId());
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
