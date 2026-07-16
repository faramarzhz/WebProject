package server;

import cli.AdminCLI;
import network.Server;

class ServerThread extends Thread {
    private Server server;
    public ServerThread(Server server) {
        this.server = server;
    }
    public void run() {
        server.start();
    }
}

public class Main {
    public static void main(String[] args) {
        Server server = new Server(8080);
        ServerThread serverThread = new ServerThread(server);
        serverThread.start();
        System.out.println("WebChat Backend Server successfully started on port 8080");

        AdminCLI cli = new AdminCLI(server);
        cli.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
        }
    }
}