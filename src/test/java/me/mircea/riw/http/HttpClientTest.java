package me.mircea.riw.http;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class HttpClientTest {
    @Test
    public void shouldGetPage() throws URISyntaxException, IOException {
        HttpClient client = new HttpClient();

        HttpRequest simpleGet = HttpRequest.newBuilder()
                .get()
                .uri("https://www.google.ro/imghp?hl=en&tab=wi")
                //.uri("http://riweb.tibeica.com/crawl")
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; dmcBot/1.0)")
                .addHeader("Connection", "close")
                .build();

        System.out.print(simpleGet.toString());
        HttpResponse response = client.send(simpleGet);


        assertNotNull(response);
        assertEquals(200, response.status());

        System.out.print(response.toString());
    }
}
