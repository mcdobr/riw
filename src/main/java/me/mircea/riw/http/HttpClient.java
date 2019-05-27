package me.mircea.riw.http;

import me.mircea.riw.dns.DnsClient;

import java.io.*;
import java.net.*;

public class HttpClient implements AutoCloseable {
    private static final int HTTP_PORT = 80;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; dmcBot/1.0)";

    private DnsClient dnsClient;
    private Socket socket;

    public HttpClient() {
    }

    public HttpResponse send(HttpRequest request) throws IOException {
        if (socket == null || !socket.isConnected()) {
            socket = new Socket();
            socket.connect(new InetSocketAddress(request.uri().getHost(), HTTP_PORT));
        }

        PrintWriter socketWriter = new PrintWriter(socket.getOutputStream());
        socketWriter.write(request.toString());
        socketWriter.flush();

        HttpResponse response =  HttpResponse.readFrom(socket.getInputStream());
        String connectionInstruction = response.headers().getOrDefault("Connection", "close");
        if (connectionInstruction.equals("close")) {
            socket.close();
            socket = null;
        }


        return response;
    }

    @Override
    public void close() throws Exception {
        if (!socket.isClosed()) {
            socket.close();
        }
    }
}
