package network;

public class ResponseBuilder {
    public static String buildResponse(int code, String badanePayam) {
        String status = "";
        if (code == 200)
            status = "OK";
        else if (code == 201)
            status = "Created";
        else if (code == 204)
            status = "No Content";
        else if (code == 400)
            status = "Bad Request";
        else if (code == 401)
            status = "Unauthorized";
        else if (code == 403)
            status = "Forbidden";
        else if (code == 404)
            status = "Not Found";
        else if (code == 409)
            status = "Conflict";
        else if (code == 429)
            status = "Too Many Requests";
        else
            status = "Internal Server Error";
        byte[] bytebadane = badanePayam.getBytes();
        return "HTTP/1.1 "+code+" "+status+"\r\n"+"Content-Type: application/json; charset=UTF-8\r\n"+"Content-Length: "+bytebadane.length+"\r\n"+"Connection: close\r\n"+"\r\n"+badanePayam;
    }
}