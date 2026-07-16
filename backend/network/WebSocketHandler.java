package network;
import util.WebSocketKeyGenerator;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import models.User;

public class WebSocketHandler {
    private Socket socket;
    private String kelidmorgar;
    private Server server;
    private String userid;

    public WebSocketHandler(Socket socket, String kelidmorgar, Server server, String userId) {
        this.socket = socket;
        this.kelidmorgar = kelidmorgar;
        this.server = server;
        this.userid = userId;
    }

    public void handle() {
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            String kelidserver = WebSocketKeyGenerator.generateAcceptKey(kelidmorgar);
            String javab = "HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n" + "Sec-WebSocket-Accept: " + kelidserver + "\r\n" + "\r\n";
            out.write(javab.getBytes(StandardCharsets.UTF_8));
            out.flush();
            server.getActiveConnections().put(userid, this);
            System.out.println("WebSocket connection established for user: " + userid);
            while (true) {
                String payam = readFrame(in);
                if (payam == null)
                    break;
            }
        } catch (IOException e) {
            System.err.println("WebSocket connection closed");
        } finally {
            server.getActiveConnections().remove(userid);
            User user = server.getUserService().getUserById(userid);
            if (user != null)
                user.setLastSeen(System.currentTimeMillis());
            try {
                socket.close();
            } catch (IOException e) {
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

    private int xor(int a, int b) {
        int result = 0;
        int tavan = 1;
        for (int i = 0; i < 8; i++) {
            int bitA = a % 2;
            int bitB = b % 2;
            if (bitA != bitB) {
                result = result + tavan;
            }
            a = a / 2;
            b = b / 2;
            tavan = tavan * 2;
        }
        return result;
    }

    private String readFrame(InputStream in) throws IOException {
        int byteaval = in.read();
        if (byteaval == -1)
            return null;
        int bytedovom = in.read();
        int tool = bytedovom;
        if (tool >= 128) {
            tool = tool - 128;
        }
        byte[] kelidmask = new byte[4];
        for (int i = 0; i < 4; i++) {
            kelidmask[i] = (byte) in.read();
        }
        byte[] data = new byte[tool];
        for (int i = 0; i < tool; i++) {
            int encode = in.read();
            int bytemask = kelidmask[i % 4];
            if (bytemask < 0) {
                bytemask = bytemask + 256;
            }
            data[i] = (byte) xor(encode, bytemask);
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private void sendFrame(OutputStream out, String message) throws IOException {
        byte[] bytepayam = message.getBytes(StandardCharsets.UTF_8);
        int tool = bytepayam.length;
        out.write(129);
        if (tool <= 125) {
            out.write(tool);
        } else if (tool <= 65535) {
            out.write(126);
            out.write((tool >> 8) & 0xFF);
            out.write(tool & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) {
                out.write((int) ((tool >> (8 * i)) & 0xFF));
            }
        }
        out.write(bytepayam);
        out.flush();
    }
}
