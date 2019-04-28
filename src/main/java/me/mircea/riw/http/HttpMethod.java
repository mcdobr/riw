package me.mircea.riw.http;

public enum HttpMethod {
    OPTIONS("OPTIONS"),
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    TRACE("TRACE"),
    CONNECT("CONNECT");

    String method;
    HttpMethod(String method) {
        this.method = method;
    }
}
