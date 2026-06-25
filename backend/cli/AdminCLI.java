package cli;

import models.Chat;
import models.Group;
import models.Message;
import models.User;
import network.Server;
import util.PasswordEncryptor;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

public class AdminCLI {
    private Server server;
    private Scanner scanner;
    // الگوهای رجکس برای بررسی ولیدیشن پسورد
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*]");

    public AdminCLI(Server server) {
        this.server = server;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.print("\nEnter Admin Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter Admin Password: ");
        String password = scanner.nextLine().trim();

        if (username.equals("admin") && password.equals("adminpass")) {
            System.out.println("Admin authenticated.");
            showHelp();
            runCLI();
        } else {
            System.out.println("Invalid admin credentials.");
        }
    }

    private void showHelp() {
        System.out.println("         Admin CLI - Commands           ");
        System.out.println("");
        System.out.println("  listusers              - List all users");
        System.out.println("  adduser                - Add a new user");
        System.out.println("  deleteuser <userId>    - Delete a user");
        System.out.println("  listgroups             - List all groups");
        System.out.println("  addgroup               - Add a new group");
        System.out.println("  deletegroup <groupId>  - Delete a group");
        System.out.println("  addmember <groupId>    - Add member to group");
        System.out.println("  removemember <groupId> - Remove member from group");
        System.out.println("  reports                - Show reported messages");
        System.out.println("  help                   - Show this help");
        System.out.println("  exit                   - Exit admin panel");
    }

    private void runCLI() {
        while (true) {
            System.out.print("\nadmin> ");
            String input = scanner.nextLine().trim().toLowerCase();
            String[] parts = input.split("\\s+", 2);
            String command = parts[0];
            String arg = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "listusers":
                    listUsers();
                    break;
                case "adduser":
                    addUser();
                    break;
                case "deleteuser":
                    deleteUser(arg);
                    break;
                case "listgroups":
                    listGroups();
                    break;
                case "addgroup":
                    addGroup();
                    break;
                case "deletegroup":
                    deleteGroup(arg);
                    break;
                case "addmember":
                    addMember(arg);
                    break;
                case "removemember":
                    removeMember(arg);
                    break;
                case "reports":
                    showReports();
                    break;
                case "help":
                    showHelp();
                    break;
                case "exit":
                    System.out.println("Exiting admin panel.");
                    return;
                default:
                    System.out.println("Unknown command: '" + command + "'. Type 'help' for commands.");
            }
        }
    }


    // User Commands
    private void listUsers() {
        HashMap<String, User> users = server.getUsers();
        if (users.isEmpty()) {
            System.out.println("No users registered.");
            return;
        }
        System.out.println("\n[Users]");
        for (User u : users.values()) {
            System.out.println("  ID: " + u.getUserId() +
                    " | Username: " + u.getUsername() +
                    (u.isBlocked() ? " | [BLOCKED]" : ""));
        }
    }

    private void addUser() {
        HashMap<String, User> users = server.getUsers();
        System.out.print("User ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Username: ");
        String name = scanner.nextLine().trim();
        System.out.print("Password: ");
        String pass = scanner.nextLine().trim();

        if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }
        if (users.containsKey(id)) {
            System.out.println("User ID already exists.");
            return;
        }

        if (!UPPERCASE_PATTERN.matcher(pass).find()) {
            System.out.println("Password must contain at least one uppercase letter.");
            return;
        }
        if (!LOWERCASE_PATTERN.matcher(pass).find()) {
            System.out.println("Password must contain at least one lowercase letter.");
            return;
        }
        if (!DIGIT_PATTERN.matcher(pass).find()) {
            System.out.println("Password must contain at least one digit.");
            return;
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(pass).find()) {
            System.out.println("Password must contain at least one special character (!@#$%^&*).");
            return;
        }
        if (pass.toLowerCase().contains(name.toLowerCase())) {
            System.out.println("Password cannot contain the username.");
            return;
        }

        users.put(id, new User(name, PasswordEncryptor.hashPassword(pass), id));
        System.out.println("User added: " + id);
    }

    private void deleteUser(String userId) {
        if (userId.isEmpty()) {
            System.out.print("User ID to delete: ");
            userId = scanner.nextLine().trim();
        }
        HashMap<String, User> users = server.getUsers();
        if (users.remove(userId) != null) {
            System.out.println("User deleted: " + userId);
        } else {
            System.out.println("User not found.");
        }
    }


    // Group Commands
    private void listGroups() {
        HashMap<String, Group> groups = server.getGroups();
        if (groups.isEmpty()) {
            System.out.println("No groups yet.");
            return;
        }
        System.out.println("\n[Groups]");
        for (Group g : groups.values()) {
            System.out.println("  ID: " + g.getGroupId() +
                    " | Name: " + g.getGroupName() +
                    " | Members: " + g.getMembers().size());
            System.out.println("    Members: " + g.getMembers());
        }
    }

    private void addGroup() {
        HashMap<String, Group> groups = server.getGroups();
        System.out.print("Group ID: ");
        String gid = scanner.nextLine().trim();
        System.out.print("Group Name: ");
        String gname = scanner.nextLine().trim();
        if (gid.isEmpty() || gname.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }
        if (groups.containsKey(gid)) {
            System.out.println("Group ID already exists.");
        } else {
            groups.put(gid, new Group(gid, gname));
            System.out.println("Group created: " + gid);
        }
    }

    private void deleteGroup(String groupId) {
        if (groupId.isEmpty()) {
            System.out.print("Group ID to delete: ");
            groupId = scanner.nextLine().trim();
        }
        HashMap<String, Group> groups = server.getGroups();
        if (groups.remove(groupId) != null) {
            System.out.println("Group deleted.");
        } else {
            System.out.println("Group not found.");
        }
    }

    private void addMember(String groupId) {
        if (groupId.isEmpty()) {
            System.out.print("Group ID: ");
            groupId = scanner.nextLine().trim();
        }
        if (groupId.isEmpty()) {
            System.out.println("Group ID cannot be empty.");
            return;
        }
        HashMap<String, Group> groups = server.getGroups();
        Group g = groups.get(groupId);
        if (g == null) {
            System.out.println("Group not found.");
            return;
        }
        System.out.print("User ID to add: ");
        String uid = scanner.nextLine().trim();
        if (uid.isEmpty()) {
            System.out.println("User ID cannot be empty.");
            return;
        }
        if (!server.getUsers().containsKey(uid)) {
            System.out.println("User '" + uid + "' does not exist.");
            return;
        }
        if (g.getMembers().contains(uid)) {
            System.out.println("User is already a member of this group.");
            return;
        }
        g.getMembers().add(uid);
        System.out.println("Member added.");
    }

    private void removeMember(String groupId) {
        if (groupId.isEmpty()) {
            System.out.print("Group ID: ");
            groupId = scanner.nextLine().trim();
        }
        if (groupId.isEmpty()) {
            System.out.println("Group ID cannot be empty.");
            return;
        }
        HashMap<String, Group> groups = server.getGroups();
        Group g = groups.get(groupId);
        if (g == null) {
            System.out.println("Group not found.");
            return;
        }
        System.out.print("User ID to remove: ");
        String uid = scanner.nextLine().trim();
        if (uid.isEmpty()) {
            System.out.println("User ID cannot be empty.");
            return;
        }
        if (!server.getUsers().containsKey(uid)) {
            System.out.println("User '" + uid + "' does not exist.");
            return;
        }
        if (g.getMembers().remove(uid)) {
            System.out.println("Member removed.");
        } else {
            System.out.println("User is not a member of this group.");
        }
    }


    // Reports
    private void showReports() {
        HashMap<String, Chat> chats = server.getChats();
        HashMap<String, Group> groups = server.getGroups();
        boolean found = false;

        System.out.println("\n─── Reported Messages ───");
        System.out.println("[Private Chats]");
        for (Chat chat : chats.values()) {
            for (Message msg : chat.getMessages()) {
                if (msg.isReported()) {
                    System.out.println("  Chat: " + chat.getChatId() +
                            " | Sender: " + msg.getSenderId() +
                            " | Content: " + msg.getContent());
                    found = true;
                }
            }
        }
        System.out.println("[Groups]");
        for (Group group : groups.values()) {
            for (Message msg : group.getMessages()) {
                if (msg.isReported()) {
                    System.out.println("  Group: " + group.getGroupName() +
                            " | Sender: " + msg.getSenderId() +
                            " | Content: " + msg.getContent());
                    found = true;
                }
            }
        }
        if (!found)
            System.out.println("No reported messages.");
    }
}