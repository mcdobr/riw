package me.mircea.riw.http;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class HttpClient implements AutoCloseable {
    private static final int HTTP_PORT = 80;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; dmcBot/1.0)";
    private final Socket socket;

    public HttpClient() {
        this.socket = new Socket();
    }


    //TODO: grija la socket, în unele cazuri ar trebui închis, iar în altele refolosit
    public HttpResponse send(HttpRequest request) throws UnknownHostException, IOException {
        //Socket socket = new Socket(request.uri().getHost(), HTTP_PORT);

        socket.connect(new InetSocketAddress(request.uri().getHost(), HTTP_PORT));


        PrintWriter socketWriter = new PrintWriter(socket.getOutputStream());
        socketWriter.write(request.toString());
        socketWriter.flush();



        return HttpResponse.readFrom(socket.getInputStream());
    }

    @Override
    public void close() throws Exception {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
