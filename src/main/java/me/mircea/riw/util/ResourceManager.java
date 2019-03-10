package me.mircea.riw.util;

import me.mircea.riw.model.DocumentContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceManager {
    public static Set<String> readAllLines(String resourceName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        BufferedReader br = new BufferedReader(new InputStreamReader(classloader.getResourceAsStream(resourceName)));
        return br.lines().map(String::trim).collect(Collectors.toSet());
    }

    public static Document readHtmlResource(String resourcePath, String url) throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(resourcePath);

        return Jsoup.parse(is, null, url);
    }
}
