package network;

import util.WebSocketKeyGenerator;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class WebSocketHandler {
    private Socket socket;
    private String clientKey;
    private Server server;
    private String userId;

    public WebSocketHandler(Socket socket, String clientKey, Server server, String userId) {
        this.socket = socket;
        this.clientKey = clientKey;
        this.server = server;
        this.userId = userId;
    }

    public void handle() {
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            String acceptKey = WebSocketKeyGenerator.generateAcceptKey(clientKey);

            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                    "\r\n";

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();

            server.getActiveConnections().put(userId, this);
            System.out.println("WebSocket connection established for user: " + userId);

            while (true) {
                String message = readFrame(in);
                if (message == null) {
                    break;
                }
                System.out.println("Received: " + message);
            }

        } catch (IOException e) {
            System.err.println("WebSocket connection closed");
        } finally {
            server.getActiveConnections().remove(userId);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void sendMessage(String message) {
        try {
            OutputStream out = socket.getOutputStream();
            sendFrame(out, message);
        } catch (IOException e) {
            System.err.println("Could not send message to client");
        }
    }

    private int myXor(int a, int b) {
        int result = 0;
        int power = 1;
        for (int i = 0; i < 8; i++) {
            int bitA = a % 2;
            int bitB = b % 2;
            if (bitA != bitB) {
                result = result + power;
            }
            a = a / 2;
            b = b / 2;
            power = power * 2;
        }
        return result;
    }

    private String readFrame(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte == -1)
            return null;

        int secondByte = in.read();
        int length = secondByte;
        if (length >= 128) {
            length = length - 128;
        }

        byte[] maskKey = new byte[4];
        for (int i = 0; i < 4; i++) {
            maskKey[i] = (byte) in.read();
        }

        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            int encodedByte = in.read();
            int maskByte = maskKey[i % 4];
            if (maskByte < 0) {
                maskByte = maskByte + 256;
            }
            data[i] = (byte) myXor(encodedByte, maskByte);
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    private void sendFrame(OutputStream out, String message) throws IOException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        int length = messageBytes.length;

        out.write(129);
        out.write(length);
        out.write(messageBytes);
        out.flush();
    }
}