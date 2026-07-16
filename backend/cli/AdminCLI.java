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
    private Pattern uppCase = Pattern.compile("[A-Z]");
    private Pattern lowCase = Pattern.compile("[a-z]");
    private Pattern adad = Pattern.compile("[0-9]");
    private Pattern ch = Pattern.compile("[!@#$%^&*]");

    public AdminCLI(Server server) {
        this.server = server;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println();
        System.out.println("Enter Admin Username: ");
        String username = scanner.nextLine();
        System.out.println("Enter Admin Password: ");
        String password = scanner.nextLine();
        if (username.equals("faramarz") && password.equals("faramarz07")) {
            System.out.println("Admin authenticated.");
            showHelp();
            runCLI();
        } 
        else
        System.out.println("Invalid admin credentials.");
    }

    private void showHelp() {
        System.out.println("cli commands");
        System.out.println();
        System.out.println("listusers");
        System.out.println("adduser");
        System.out.println("deleteuser");
        System.out.println("listgroups");
        System.out.println("addgroup");
        System.out.println("deletegroup");
        System.out.println("addmember");
        System.out.println("removemember");
        System.out.println("reports");
        System.out.println("help");
        System.out.println("exit");
    }
    private void runCLI() {
        while (true) {
            System.out.println();
            System.out.print("admin> ");
            String command = scanner.nextLine();
            switch (command) {
                case "listusers":
                    listUsers();
                    break;
                case "adduser":
                    addUser();
                    break;
                case "deleteuser":
                    deleteUser();
                    break;
                case "listgroups":
                    listGroups();
                    break;
                case "addgroup":
                    addGroup();
                    break;
                case "deletegroup":
                    deleteGroup();
                    break;
                case "addmember":
                    addMember();
                    break;
                case "removemember":
                    removeMember();
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

    private void deleteUser() {
        System.out.print("User ID to delete: ");
        String userId = scanner.nextLine();
        HashMap<String, User> users = server.getUsers();
        if (users.remove(userId) != null) {
            System.out.println("User deleted: " + userId);
        } else {
            System.out.println("User not found.");
        }
    }
    private void listUsers() {
        HashMap<String, User> users = server.getUsers();
        if (users.isEmpty()) {
            System.out.println("No users registered.");
            return;
        }
        System.out.println();
        System.out.println("Users:");
        for (User user : users.values()) {
            String status = "";
            if(user.isBlocked())
            status = " | [BLOCKED]";
            System.out.println("  ID: " + user.getUserId() + " | Username: " + user.getUsername() + status);
        }
    }
    private void addUser() {
        HashMap<String, User> users = server.getUsers();
        System.out.print("User ID: ");
        String id = scanner.nextLine();
        System.out.print("Username: ");
        String name = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();
        if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }
        if (users.containsKey(id)) {
            System.out.println("User ID already exists.");
            return;
        }
        if (!uppCase.matcher(pass).find()) {
            System.out.println("Password must contain at least one uppercase letter.");
            return;
        }
        if (!lowCase.matcher(pass).find()) {
            System.out.println("Password must contain at least one lowercase letter.");
            return;
        }
        if (!adad.matcher(pass).find()) {
            System.out.println("Password must contain at least one digit.");
            return;
        }
        if (!ch.matcher(pass).find()) {
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

    private void addMember() {
        System.out.print("Group ID: ");
        String groupId = scanner.nextLine();
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
        String uid = scanner.nextLine();
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
    private void removeMember() {
        System.out.print("Group ID: ");
        String groupId = scanner.nextLine();
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
        String uid = scanner.nextLine();
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

    private void listGroups() {
        HashMap<String, Group> groups = server.getGroups();
        if (groups.isEmpty()) {
            System.out.println("No groups yet.");
            return;
        }
        System.out.println();
        System.out.println("[Groups]");
        for (Group g : groups.values()) {
            System.out.println("  ID: " + g.getGroupId() +
                    " | Name: " + g.getName() +
                    " | Members: " + g.getMembers().size());
            System.out.println("    Members: " + g.getMembers());
        }
    }

    private void addGroup() {
        HashMap<String, Group> groups = server.getGroups();
        System.out.print("Group ID: ");
        String gid = scanner.nextLine();
        System.out.print("Group Name: ");
        String gname = scanner.nextLine();
        System.out.print("Creator User ID: ");
        String creatorId = scanner.nextLine();
        if (gid.isEmpty() || gname.isEmpty() || creatorId.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }
        if (groups.containsKey(gid)) {
            System.out.println("Group ID already exists.");
            return;
        }
        if (!server.getUsers().containsKey(creatorId)) {
            System.out.println("Creator user '" + creatorId + "' does not exist.");
            return;
        }
        Group newGroup = new Group(gid, gname, creatorId);
        newGroup.getMembers().add(creatorId);
        groups.put(gid, newGroup);
        System.out.println("Group created: " + gid);
    }

    private void deleteGroup() {
        System.out.print("Group ID to delete: ");
        String groupId = scanner.nextLine();
        HashMap<String, Group> groups = server.getGroups();
        if (groups.remove(groupId) != null)
            System.out.println("Group deleted.");
        else
        System.out.println("Group not found.");    
    }

    private void showReports() {
        HashMap<String, Chat> chats = server.getChats();
        HashMap<String, Group> groups = server.getGroups();
        boolean found = false;
        System.out.println();
        System.out.println("Reported Messages");
        System.out.println("[Private Chats]");
        for (Chat chat : chats.values()) {
            for (Message msg : chat.getMessages()) {
                if (msg.isReported()) {
                    System.out.println("  Chat: " + chat.getChatId() + " | Sender: " + msg.getSenderId() + " | Content: " + msg.getContent());
                    found = true;
                }
            }
        }
        System.out.println("[Groups]");
        for (Group group : groups.values()) {
            for (Message msg : group.getMessages()) {
                if (msg.isReported()) {
                    System.out.println("  Group: " + group.getName() + " | Sender: " + msg.getSenderId() + " | Content: " + msg.getContent());
                    found = true;
                }
            }
        }
        if (!found)
            System.out.println("No reported messages.");
    }
}