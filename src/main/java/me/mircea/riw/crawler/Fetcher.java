package me.mircea.riw.crawler;

import me.mircea.riw.http.HttpClient;
import me.mircea.riw.http.HttpRequest;
import me.mircea.riw.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Fetcher implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fetcher.class);

    private final CrawlController controller;
    private final HttpClient httpClient;
    private final String targetHost;
    private final BlockingQueue<URI> downloadQueue;
    private boolean finished;

    public Fetcher(CrawlController controller, URI seed) {
        this.controller = controller;
        this.httpClient = new HttpClient();
        this.targetHost = seed.getHost();
        this.downloadQueue = new LinkedBlockingQueue<>();
        this.finished = false;
    }

    public boolean isFinished() {
        return finished;
    }

    public void fetch(URI uri) {
        this.downloadQueue.add(uri);
    }

    @Override
    public void run() {
        while (!finished) {
            URI uri = tryToGetUri();
            if (uri != null) {
                tryToDownloadAndFollowLinks(uri);
            } else {
                finished = true;
            }
        }
    }

    private URI tryToGetUri() {
        URI uri = null;
        try {
            uri = downloadQueue.poll(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Fetcher thread was interrupted {}", e);
        }
        return uri;
    }

    private void tryToDownloadAndFollowLinks(URI uri) {
        try {
            downloadAndFollowLinks(uri);
        } catch (IOException e) {
            LOGGER.warn("Could not retrieve URI {}", uri);
        }
    }

    private void downloadAndFollowLinks(URI uri) throws IOException {
        // TODO: trebuie inchis sau refolosit socket-ul
        HttpRequest getRequest = HttpRequest.newBuilder()
                .get()
                .uri(uri)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; dmcBot/1.0)")
                .addHeader("Connection", "close")
                .build();


        // TODO: check on own http client if it works
        HttpResponse response = httpClient.send(getRequest);
        LOGGER.info("Dowloading document at URI {}", uri);

        Document htmlDoc = Jsoup.parse(response.getBody(), uri.toString());
        Elements links = htmlDoc.select("a[href]");

        Element metaRobotsTag = htmlDoc.selectFirst("meta[name='robots']");
        Set<String> instructions = Collections.emptySet();
        if (metaRobotsTag != null) {
            instructions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            instructions.addAll(Arrays.asList(metaRobotsTag.attr("content").split(",")));
        }

        if (shouldFollowLinks(instructions)) {
            followLinks(links);
            LOGGER.info("Following links from document at URI {}", uri);
        }

        if (shouldIndex(instructions)) {
            saveDocument(uri, htmlDoc);
            LOGGER.info("Saving document at URI {}", uri);
        }
    }

    private boolean shouldFollowLinks(Set<String> instructions) {
        return !instructions.contains("nofollow") && !instructions.contains("none");
    }

    private boolean shouldIndex(Set<String> instructions) {
        return !instructions.contains("noindex") && !instructions.contains("none");
    }

    private void followLinks(Elements links) {
        for (Element link : links) {
            try {
                String str = link.absUrl("href");
                if (!str.isEmpty()) {
                    URI other = new URI(str);

                    if (!hasBeenVisited(other) && other.getHost().equals(this.targetHost)) {
                        this.controller.add(other);
                    }
                }
            } catch (URISyntaxException e) {
                LOGGER.info("Reconstructed URI was malformed {}", e);
            }
        }
    }

    private void saveDocument(URI uri, org.jsoup.nodes.Document jsoupDoc) {
        Path fileSystemTranslatedPath = translateToPath(uri);

        BufferedWriter bw = null;
        try {
            Files.createDirectories(fileSystemTranslatedPath.getParent());
            bw = Files.newBufferedWriter(fileSystemTranslatedPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            bw.write(jsoupDoc.html());
            LOGGER.info("Saving URI {} to file {}", uri, fileSystemTranslatedPath);
        } catch (IOException e) {
            LOGGER.warn("An I/O exception occured when trying to save the files on URI {}: {}", uri, e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LOGGER.warn("An exception occured while trying to close a writer {}", e);
                }
            }
        }
    }

    private boolean hasBeenVisited(URI uri) {
        Path fileSystemTranslatedPath = translateToPath(uri);
        return Files.exists(fileSystemTranslatedPath);
    }

    private Path translateToPath(URI uri) {
        Path pathOfUri = Paths.get(controller.getDestination().toString(), uri.getHost(), uri.getPath());
        if (pathOfUri.endsWith("/")) {
            pathOfUri = Paths.get(pathOfUri.toString(), "index.html");
        }
        if (uri.getQuery() != null) {
            pathOfUri = Paths.get(pathOfUri.toString(), uri.getQuery());
        }
        return pathOfUri;
    }
}
