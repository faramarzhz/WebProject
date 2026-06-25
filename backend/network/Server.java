package network;

import models.Chat;
import models.Group;
import models.User;
import services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {
    private int port;
    private ServerSocket serverSocket;
    private volatile boolean isRunning; // وضعیت روشن بودن سرور مجاز در مولتی‌ترد

    // داده‌های اصلی برنامه - اشتراکی بین تمام thread ها
    private final HashMap<String, User> users;
    private final HashMap<String, Group> groups;
    // سرویس‌های بک‌اند
    private final AuthService authService;
    private final UserService userService;
    private final MessageService messageService;
    private final ChatService chatService;
    private final GroupService groupService;

    public Server(int port) {
        this.port = port;
        isRunning = false;
        users = new HashMap<>();
        groups = new HashMap<>();
        authService = new AuthService(users);
        userService = new UserService(users);
        messageService = new MessageService();
        chatService = new ChatService();
        groupService = new GroupService();
    }

    // راه‌اندازی سرور سوکت روی پورت مشخص شده و پذیرش کلاینت‌ها
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Server listening on port " + port);
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept(); // منتظر اتصال کلاینت جدید می‌ماند
                    // اجرای هر client در یک thread جداگانه برای پشتیبانی هم‌زمان
                    new Thread(new ClientHandler(clientSocket, this)).start();
                } catch (IOException e) {
                    if (isRunning)
                        System.err.println("Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // خاموش کردن سرور و بستن سوکت اصلی
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    // ─── Getters ───────────────────────────────────────────────
    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public HashMap<String, User> getUsers() {
        return users;
    }

    public HashMap<String, Group> getGroups() {
        return groups;
    }

    public HashMap<String, Chat> getChats() {
        return chatService.getAllChats();
    }
}