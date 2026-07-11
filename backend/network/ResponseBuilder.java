package network;

public class ResponseBuilder {
    public static String buildResponse(int statusCode, String jsonBody) {
        String statusText = getStatusText(statusCode);
        byte[] bodyBytes = jsonBody.getBytes();
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                jsonBody;
    }

    public static String ok(String jsonBody) {
        return buildResponse(200, jsonBody);
    }

    public static String created(String jsonBody) {
        return buildResponse(201, jsonBody);
    }

    public static String error(int code, String message) {
        return buildResponse(code, "{\"error\":\"" + message + "\"}");
    }

    private static String getStatusText(int code) {
        switch (code) {
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 204:
                return "No Content";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 409:
                return "Conflict";
            case 429:
                return "Too Many Requests";
            default:
                return "Internal Server Error";
        }
    }
}