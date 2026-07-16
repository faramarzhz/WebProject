package network;

public class ResponseBuilder {
    public static String buildResponse(int statusCode, String jsonBody) {
        String statusText = "";
        if (statusCode == 200)
            statusText = "OK";
        else if (statusCode == 201)
            statusText = "Created";
        else if (statusCode == 204)
            statusText = "No Content";
        else if (statusCode == 400)
            statusText = "Bad Request";
        else if (statusCode == 401)
            statusText = "Unauthorized";
        else if (statusCode == 403)
            statusText = "Forbidden";
        else if (statusCode == 404)
            statusText = "Not Found";
        else if (statusCode == 409)
            statusText = "Conflict";
        else if (statusCode == 429)
            statusText = "Too Many Requests";
        else
            statusText = "Internal Server Error";

        byte[] bodyBytes = jsonBody.getBytes();
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                jsonBody;
    }
}