package me.mircea.riw.http;

import com.google.common.collect.ImmutableMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class HttpResponse {
    private static final int MAX_REDIRECTS;

    static {
        MAX_REDIRECTS = 3;
    }

    private final HttpVersion version;
    private final int status;
    private final String reasonPhrase;
    private final ImmutableMap<String, String> headers;
    private final String body;

    private HttpResponse(Builder responseBuilder) {
        this.version = responseBuilder.version;
        this.status = responseBuilder.status;
        this.reasonPhrase = responseBuilder.reasonPhrase;
        this.headers = ImmutableMap.copyOf(responseBuilder.headers);
        this.body = responseBuilder.bodyBuilder.toString();
    }

    public HttpVersion version() {
        return version;
    }

    public int status() {
        return status;
    }

    public ImmutableMap<String, String> headers() {
        return headers;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HttpResponse{");
        sb.append("version=").append(version);
        sb.append(",\n status=").append(status);
        sb.append(",\n reasonPhrase='").append(reasonPhrase).append('\'');
        sb.append(",\n headers=").append(headers);
        sb.append(",\n body=").append(body);
        sb.append('}');
        return sb.toString();
    }

    public static HttpResponse readFrom(InputStream is) throws IOException {
        Builder builder = new Builder();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String statusLine = br.readLine();

        Scanner scanner = new Scanner(statusLine);
        String versionStr = scanner.next();
        builder.version(HttpVersion.fromString(versionStr))
                .status(scanner.nextInt())
                .reasonPhrase(scanner.nextLine().trim());

        String header;
        while (!(header = br.readLine().trim()).isEmpty()) {
            String[] headerParts = header.split(":", 2);
            builder.addHeader(headerParts[0], headerParts[1]);
        }

        String bodyLine;
        while ((bodyLine = br.readLine()) != null) {
            builder.bodyBuilder.append(bodyLine).append(System.lineSeparator());
        }

        return builder.build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getBody() {
        return body;
    }

    public static class Builder {
        private HttpVersion version;
        private int status;
        private String reasonPhrase;
        private Map<String, String> headers;
        private StringBuilder bodyBuilder;

        public Builder() {
            this.headers = new HashMap<>();
            this.bodyBuilder = new StringBuilder();
        }

        public Builder version(HttpVersion version) {
            this.version = version;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder reasonPhrase(String reasonPhrase) {
            this.reasonPhrase = reasonPhrase;
            return this;
        }

        public Builder addHeader(String key, String value) {
            headers.put(key.trim(), value.trim());
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}
