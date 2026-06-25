package network;

public class ResponseBuilder {
    // هدرهای CORS برای مجاز کردن ارتباط دامنه فرانت‌اند با پورت بک‌اند روی مروگر
    private static final String CORS_HEADERS = "Access-Control-Allow-Origin: *\r\n"
            + "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n"
            + "Access-Control-Allow-Headers: Content-Type, Authorization\r\n";

    // متد تولید پکت‌های متنی استاندارد پروتکل HTTP/1.1
    public static String buildResponse(int statusCode, String jsonBody) {
        String statusText = getStatusText(statusCode);
        byte[] bodyBytes;
        try {
            bodyBytes = jsonBody.getBytes("UTF-8");
        } catch (Exception e) {
            bodyBytes = jsonBody.getBytes();
        }
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                CORS_HEADERS +
                "Connection: close\r\n" +
                "\r\n" +
                jsonBody;
    }

    public static String buildOptionsResponse() {
        return "HTTP/1.1 204 No Content\r\n" +
                CORS_HEADERS +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    public static String ok(String jsonBody) {
        return buildResponse(200, jsonBody);
    }

    public static String created(String jsonBody) {
        return buildResponse(201, jsonBody);
    }

    public static String error(int code, String message) {
        return buildResponse(code, "{\"error\":\"" + escapeJson(message) + "\"}");
    }

    // متن های JSON خنثی‌سازی کاراکترهای مخرب یا خاص برای جلوگیری از خراب شدن ساختار
    public static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    // گرفتن متن وضعیت خطاها بر اساس کدهای استاندارد شبکه
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