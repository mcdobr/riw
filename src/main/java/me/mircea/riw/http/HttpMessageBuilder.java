package me.mircea.riw.http;

class HttpMessageBuilder {
    private StringBuilder builder;

    public HttpMessageBuilder() {
        this.builder = new StringBuilder();
    }

    public HttpMessageBuilder addHeader(String key, String value) {
        return addLine(String.format("%s: %s", key, value));

    }

    public HttpMessageBuilder addLine() {
        return this.addLine("");
    }

    public HttpMessageBuilder addLine(String str) {
        builder.append(str.trim())
                .append("\r\n");
        return this;
    }

    public String build() {
        return builder.toString();
    }
}
