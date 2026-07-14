package network;

import models.Chat;
import models.Group;
import models.User;
import services.*;
import database.Database;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {
    private int port;
    private ServerSocket serverSocket;
    private volatile boolean isRunning;

    private HashMap<String, User> users;
    private HashMap<String, Group> groups;
    private AuthService authService;
    private UserService userService;
    private MessageService messageService;
    private ChatService chatService;
    private GroupService groupService;
    private HashMap<String, WebSocketHandler> activeConnections;

    public Server(int port) {
        this.port = port;
        isRunning = false;
        users = Database.loadUsers();
        groups = Database.loadGroups();
        authService = new AuthService(users);
        userService = new UserService(users);
        messageService = new MessageService();
        chatService = new ChatService();
        groupService = new GroupService();
        activeConnections = new HashMap<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Server listening on port " + port);
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
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

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException ignored) {
        }
    }

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

    public HashMap<String, WebSocketHandler> getActiveConnections() {
        return activeConnections;
    }
}