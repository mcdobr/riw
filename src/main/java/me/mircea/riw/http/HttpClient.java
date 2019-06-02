package me.mircea.riw.http;

import me.mircea.riw.dns.DnsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.time.Instant;

public class HttpClient implements AutoCloseable {
    private static final int HTTP_PORT = 80;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; dmcBot/1.0)";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);
    private static final int MAX_CONNECTION_TIMEOUT = 750;


    private DnsClient dnsClient;
    private Socket socket;

    public HttpClient() {
    }

    public HttpResponse send(HttpRequest request) throws IOException {
        HttpResponse response;
        try {
            if (socket == null || !socket.isConnected()) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(request.uri().getHost(), HTTP_PORT), MAX_CONNECTION_TIMEOUT);
            }

            PrintWriter socketWriter = new PrintWriter(socket.getOutputStream());
            socketWriter.write(request.toString());
            socketWriter.flush();

            if (request.humanFriendlyUri() != null) {
                LOGGER.trace("Requested {} resolved as {} at {}", request.humanFriendlyUri(), request.uri(), Instant.now());
            } else {
                LOGGER.trace("Requested {} at {}", request.uri(), Instant.now());
            }

            response = HttpResponse.readFrom(socket.getInputStream());
            String connectionInstruction = response.headers().getOrDefault("Connection", "close");
            if (connectionInstruction.equals("close")) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            LOGGER.warn("A connection problem with the http client on uri {}", request.uri());
            throw e;
        } finally {
            if (socket != null) {
                socket.close();
                socket = null;
            }
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
