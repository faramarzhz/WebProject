package network;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private RequestRouter ruoter;
    private boolean webSocket = false;
    private static final String masirFront = "frontend";

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        ruoter = new RequestRouter(server);
    }

    @Override
    public void run() {
        BufferedReader in = null;
        OutputStream out = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = socket.getOutputStream();
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.trim().isEmpty())
                return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 2)
                return;
            String metod = parts[0];
            String Path = parts[1];
            String path = Path;
            String query = "";
            int index = Path.indexOf('?');
            if (index != -1) {
                path = Path.substring(0, index);
                query = Path.substring(index + 1);
            }
            int length = 0;
            String kelidws = "";
            boolean wsReqest = false;
            String head;
            while ((head = in.readLine()) != null && !head.isEmpty()) {
                if (head.toLowerCase().startsWith("content-length:")) {
                    try {
                        length = Integer.parseInt(head.split(":", 2)[1].trim());
                    } catch (NumberFormatException e) {
                    }
                }
                if (head.toLowerCase().startsWith("upgrade:") && head.toLowerCase().contains("websocket")) {
                    wsReqest = true;
                }
                if (head.toLowerCase().startsWith("sec-websocket-key:")) {
                    kelidws = head.split(":", 2)[1].trim();
                }
            }
            if (wsReqest) {
                webSocket = true;
                String userId = getUserIdFromQuery(query);
                WebSocketHandler handeler = new WebSocketHandler(socket, kelidws, server, userId);
                handeler.handle();
                return;
            }
            String badane = "";
            if (length > 0) {
                StringBuilder badanesb = new StringBuilder();
                int tedadByte = 0;
                while (tedadByte < length) {
                    int ch = in.read();
                    if (ch == -1)
                        break;
                    badanesb.append((char) ch);
                    tedadByte += String.valueOf((char) ch).getBytes(StandardCharsets.UTF_8).length;
                }
                badane = badanesb.toString();
            }
            if (path.startsWith("/api/")) {
                String javab = ruoter.route(metod, path, query, badane);
                out.write(javab.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return;
            }
            serveStaticFile(out, metod, path);
        } catch (IOException e) {
        } finally {
            if (!webSocket) {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                }
                try {
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                }
                try {
                    if (!socket.isClosed())
                        socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void serveStaticFile(OutputStream out, String metod, String path) throws IOException {
        if (!metod.equals("GET") && !metod.equals("HEAD")) {
            writeNotFound(out);
            return;
        }
        String request = path;
        if (request.equals("/") || request.isEmpty()) {
            request = "/Login.html";
        }
        try {
            String decod = java.net.URLDecoder.decode(request, "UTF-8");
            if (decod.contains("..")) {
                writeNotFound(out);
                return;
            }
            Path filePath = Paths.get(masirFront, decod).normalize();
            Path frontendRoot = Paths.get(masirFront).normalize().toAbsolutePath();
            if (!filePath.toAbsolutePath().startsWith(frontendRoot)) {
                writeNotFound(out);
                return;
            }
            File file = filePath.toFile();
            if (!file.exists() || file.isDirectory()) {
                writeNotFound(out);
                return;
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            String type = getContentType(decod);
            String header = "HTTP/1.1 200 OK\r\n" + "Content-Type: " +type+ "\r\n" + "Content-Length: " + fileBytes.length + "\r\n" + "Connection: close\r\n" +"\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            if (metod.equals("GET")) {
                out.write(fileBytes);
            }
            out.flush();
        } catch (Exception e) {
            writeNotFound(out);
        }
    }

    private void writeNotFound(OutputStream out) throws IOException {
        String badane = "404 Not Found";
        String javab = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + badane.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                badane;
        out.write(javab.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String getContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html"))
            return "text/html; charset=UTF-8";
        if (lower.endsWith(".js"))
            return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".css"))
            return "text/css; charset=UTF-8";
        if (lower.endsWith(".json"))
            return "application/json; charset=UTF-8";
        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".svg"))
            return "image/svg+xml";
        if (lower.endsWith(".ico"))
            return "image/x-icon";
        return "application/octet-stream";
    }

    private String getUserIdFromQuery(String query) {
        if (query == null || query.isEmpty())
            return "";
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals("userId")) {
                return kv[1];
            }
        }
        return "";
    }
}