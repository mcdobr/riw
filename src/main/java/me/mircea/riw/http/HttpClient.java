package me.mircea.riw.http;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class HttpClient {
    private static final int HTTP_PORT = 80;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; dmcBot/1.0)";

    public HttpResponse send(HttpRequest request) throws UnknownHostException, IOException {
        Socket socket = new Socket(request.uri().getHost(), HTTP_PORT);

        PrintWriter socketWriter = new PrintWriter(socket.getOutputStream());
        socketWriter.write(request.toString());
        socketWriter.flush();

        return HttpResponse.readFrom(socket.getInputStream());
    }
}
