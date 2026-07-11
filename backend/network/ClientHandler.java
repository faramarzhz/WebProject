package network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private RequestRouter router;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        router = new RequestRouter(server);
    }

    @Override
    public void run() {
        BufferedReader in = null;
        OutputStream out = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = socket.getOutputStream();
            // خواندن خط اول درخواست
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.trim().isEmpty())
                return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 2)
                return;
            String method = parts[0];
            String fullPath = parts[1];
            String path = fullPath;
            String queryString = "";
            int qIdx = fullPath.indexOf('?');
            if (qIdx != -1) {
                path = fullPath.substring(0, qIdx);
                queryString = fullPath.substring(qIdx + 1);
            }
            int contentLength = 0;
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(headerLine.split(":", 2)[1].trim());
                    } catch (NumberFormatException ignored) {
                    }   
                }
            }

            String body = "";
            if (contentLength > 0) {
                StringBuilder bodyBuilder = new StringBuilder();
                int byteCount = 0;
                while (byteCount < contentLength) {
                    int ch = in.read();
                    if (ch == -1)
                        break;

                    bodyBuilder.append((char) ch);
                    byteCount += String.valueOf((char) ch).getBytes(StandardCharsets.UTF_8).length;
                }
                body = bodyBuilder.toString();
            }

            String response = router.route(method, path, queryString, body);
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ignored) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (IOException ignored) {
            }
            try {
                if (!socket.isClosed())
                    socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}