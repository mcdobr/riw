package me.mircea.riw.crawler;

import me.mircea.riw.http.HttpClient;
import me.mircea.riw.http.HttpRequest;
import me.mircea.riw.http.HttpResponse;
import me.mircea.riw.indexer.Indexer;
import me.mircea.riw.model.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Crawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);

    private final Indexer indexer;
    private final Queue<URI> activeQueue;
    private final Set<URI> visited;
    private final HttpClient httpClient;
    private final String targetAuthority;

    public Crawler(Indexer indexer, String seed) throws URISyntaxException {
        this(indexer, new URI(seed));
    }

    public Crawler(Indexer indexer, URI seed) {
        this.indexer = indexer;
        this.activeQueue = new LinkedList<>();
        this.activeQueue.add(seed);

        this.visited = new HashSet<>();
        this.httpClient = new HttpClient();
        this.targetAuthority = seed.getAuthority();
    }


    public void crawl() {
        while (!this.activeQueue.isEmpty()) {
            URI uri = this.activeQueue.poll();

            if (!visited.contains(uri)) {
                visited.add(uri);

                HttpRequest getRequest = HttpRequest.newBuilder()
                        .get()
                        .uri(uri)
                        .addHeader("User-Agent", "Mozilla/5.0 (compatible; dmcBot/1.0)")
                        .addHeader("Connection", "close")
                        .build();

                try {
                    /* HttpResponse response = this.httpClient.send(getRequest);
                    String body = response.getBody();
                    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(body, uri.toString());
                    */

                    org.jsoup.nodes.Document jsoupDoc = Jsoup.connect(uri.toString()).get();
                    Elements links = jsoupDoc.select("a[href]");

                    for (Element link : links) {
                        try {
                            String str = link.absUrl("href");
                            if (!str.isEmpty()) {
                                URI other = new URI(str);

                                if (other.getAuthority().equals(this.targetAuthority)) {
                                    activeQueue.add(other);
                                }
                            }
                        } catch (URISyntaxException e) {
                            LOGGER.info("URI was malformed {}", e);
                        }
                    }

                    //Document doc = new Document(uri.toString(), body);
                    Document doc = new Document(uri.toString(), jsoupDoc.text());
                    indexer.indexDocument(doc);



                    Thread.sleep(1000);
                } catch (UnknownHostException e) {
                    LOGGER.warn("Host unknown {}", e);
                } catch (IOException e) {
                    LOGGER.warn("An I/O exception occured while making the http request {}", e);
                } catch (InterruptedException e) {
                    LOGGER.error("Thread was interrupted while sleeping {}, e");
                }
            }
        }
    }
}
