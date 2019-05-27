package me.mircea.riw.crawler;

import me.mircea.riw.dns.DnsClient;
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
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Fetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fetcher.class);

    private final CrawlController controller;
    private final DnsClient dnsClient;
    private final HttpClient httpClient;
    private final String targetHost;
    private final BlockingQueue<URI> downloadQueue;


    public Fetcher(CrawlController controller, URI seed) throws SocketException, UnknownHostException {
        this.controller = controller;
        this.dnsClient = new DnsClient("8.8.8.8");
        this.httpClient = new HttpClient();
        this.targetHost = seed.getHost();
        this.downloadQueue = new ActiveDownloadQueue<>();
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void add(URI uri) {
        this.downloadQueue.add(uri);
    }

    public void fetchOne() {
        URI uri = tryToGetUri();
        if (uri != null) {
            tryToDownloadAndFollowLinks(uri);
        } else {
            controller.shutdownFetcher(this);
            LOGGER.info("Shutting down recurrent task on {} at {}", targetHost, Instant.now());
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
            LOGGER.warn("Could not retrieve URI {} at {}", uri, Instant.now());
        } catch (URISyntaxException e) {
            LOGGER.warn("Could not construct URI from {}", uri);
        }
    }

    private void downloadAndFollowLinks(URI uri) throws URISyntaxException, IOException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .withDnsClient(this.dnsClient)
                .get()
                .uri(uri)
                .addHeader("Host", uri.getHost())
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; dmcBot/1.0)")
                .addHeader("Connection", "close")
                .build();

        HttpResponse response = httpClient.send(getRequest);
        handleResponse(uri, response);
    }

    private void handleResponse(URI uri, HttpResponse response) {
        if (response.status() == 200) {
            parseSuccessfulResponse(uri, response);
        } else if (response.status() == 301) {
            // TODO: handle permanent redirect
        } else if (response.status() % 100 == 3){
            // TODO: handle other
        } else {
            LOGGER.warn("Got status {} on URI {}", response.status(), uri);
        }
    }


    private void parseSuccessfulResponse(URI uri, HttpResponse response) {
        LOGGER.info("Downloaded document at URI {} at {}", uri, Instant.now());

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
                    URI href = new URI(str);
                    href = new URI(href.getScheme(), href.getHost(), href.getPath(), href.getQuery(), null);

                    if (!hasBeenVisited(href) && href.getHost().equals(this.targetHost)) {
                        this.controller.add(href);
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
        uri = uri.normalize();

        Path pathOfUri = Paths.get(controller.getDestination().toString(), uri.getHost(), uri.getPath());
        if (isTheRootOfDomain(uri)) {
            pathOfUri = Paths.get(pathOfUri.toString(), "index.html");
        }
        if (uri.getQuery() != null) {
            pathOfUri = Paths.get(pathOfUri.toString(), uri.getQuery());
        }

        if (isMissingExtension((pathOfUri))) {
            pathOfUri = Paths.get(pathOfUri.toString() + ".html");
        }

        return pathOfUri;
    }

    private boolean isTheRootOfDomain(URI uri) {
        URI auxUri = null;
        try {
            auxUri = new URI("http", uri.getHost(), null, null);
        } catch (URISyntaxException e) {
            LOGGER.warn("URL was malformed for {}", uri);
        }
        return uri.equals(auxUri);
    }

    private boolean isMissingExtension(Path path) {
        String extension = com.google.common.io.Files.getFileExtension(path.toString());
        return extension.isEmpty();
    }
}
