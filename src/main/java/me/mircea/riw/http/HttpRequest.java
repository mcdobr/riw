package me.mircea.riw.http;

import com.google.common.collect.ImmutableMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public final class HttpRequest {
    private final HttpMethod method;
    private final HttpVersion version;
    private final URI uri;
    private final ImmutableMap<String, String> headers;

    private HttpRequest(Builder httpRequestBuilder) {
        this.method = httpRequestBuilder.method;
        this.version = httpRequestBuilder.version;
        this.uri = httpRequestBuilder.uri;
        this.headers = ImmutableMap.copyOf(httpRequestBuilder.headers);
    }

    public HttpMethod method() {
        return this.method;
    }

    public HttpVersion version() {
        return this.version;
    }

    public URI uri() {
        return this.uri;
    }

    public ImmutableMap<String, String> headers() {
        return headers;
    }

    @Override
    public String toString() {
        HttpMessageBuilder rawRequestBuilder = new HttpMessageBuilder();

        String requestLine = String.format("%s %s %s", this.method, this.uri.getPath(), this.version);
        rawRequestBuilder.addLine(requestLine);
        rawRequestBuilder.addHeader("Host", this.uri.getHost());
        this.headers.forEach((key, value) -> rawRequestBuilder.addHeader(key, value));
        rawRequestBuilder.addLine();
        return rawRequestBuilder.build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private HttpMethod method;
        private HttpVersion version;
        private URI uri;
        private Map<String, String> headers;

        public Builder() {
            this.version = HttpVersion.HTTP_1_1;
            this.headers = new HashMap<String, String>();
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }

        public Builder options() {
            this.method = HttpMethod.OPTIONS;
            return this;
        }

        public Builder get() {
            this.method = HttpMethod.GET;
            return this;
        }

        public Builder head() {
            this.method = HttpMethod.HEAD;
            return this;
        }

        public Builder post() {
            this.method = HttpMethod.POST;
            return this;
        }

        public Builder put() {
            this.method = HttpMethod.POST;
            return this;
        }

        public Builder delete() {
            this.method = HttpMethod.DELETE;
            return this;
        }

        public Builder trace() {
            this.method = HttpMethod.TRACE;
            return this;
        }

        public Builder connect() {
            this.method = HttpMethod.CONNECT;
            return this;
        }

        public Builder addHeader(String key, String value) {
            headers.put(key.trim(), value.trim());
            return this;
        }

        public Builder removeHeader(String key) {
            headers.remove(key.trim());
            return this;
        }

        public Builder uri(String uri) throws URISyntaxException {
            this.uri = new URI(uri);
            return this;
        }

        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }
    }

}
