package me.mircea.riw.http;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public enum HttpVersion {
    HTTP_1_1("HTTP/1.1"),
    HTTP_2("HTTP/2");

    private static final Map<String, HttpVersion> ENUM_MAP;

    static {
        Map<String, HttpVersion> mapper = Arrays.stream(HttpVersion.values()).collect(toMap(HttpVersion::version, identity()));
        ENUM_MAP = ImmutableMap.copyOf(mapper);
    }

    public static HttpVersion fromString(String str) {
        return ENUM_MAP.get(str);
    }

    String version;
    HttpVersion(String version) {
        this.version = version;
    }

    public String version() {
        return this.version;
    }

    @Override
    public String toString() {
        return version;
    }
}
