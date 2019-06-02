package me.mircea.riw.crawler;

import crawlercommons.robots.BaseRobotRules;
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
import java.util.*;
import java.util.concurrent.*;

public class Fetcher {
    private static final Logger LOGGER;
    private static final String USER_AGENT;
    private static final ExecutorService PARSER_THREADS;
    private static final Map<String, String> ESCAPE_SEQUENCES = new HashMap<>();

    static {
        LOGGER = LoggerFactory.getLogger(Fetcher.class);
        USER_AGENT = "RIWEB_CRAWLER";
        PARSER_THREADS = Executors.newFixedThreadPool(4);

        ESCAPE_SEQUENCES.put("\"", "%22");
        ESCAPE_SEQUENCES.put("<", "%3C");
        ESCAPE_SEQUENCES.put(">", "%3E");
        ESCAPE_SEQUENCES.put("*", "%2A");
        ESCAPE_SEQUENCES.put("/", "%2F");
        ESCAPE_SEQUENCES.put(":", "%3A");
        ESCAPE_SEQUENCES.put("?", "%3F");
        ESCAPE_SEQUENCES.put("\\", "%5C");
        ESCAPE_SEQUENCES.put("|", "%7C");
    }


    private final CrawlController controller;
    private final DnsClient dnsClient;
    private final HttpClient httpClient;
    private final String targetHost;
    private final BlockingQueue<URI> downloadQueue;
    private final BaseRobotRules rules;


    public Fetcher(CrawlController controller, URI seed, BaseRobotRules rules) throws SocketException, UnknownHostException {
        this.controller = controller;
        this.dnsClient = new DnsClient("8.8.8.8");
        this.httpClient = new HttpClient();
        this.targetHost = seed.getHost();
        this.downloadQueue = new ActiveDownloadQueue<>();
        this.rules = rules;
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
            uri = downloadQueue.poll(15, TimeUnit.SECONDS);
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
        final int MAX_TRIES = 3;

        boolean shouldTry = true;
        for (int noTry = 0; shouldTry && noTry < MAX_TRIES; ++noTry){
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .withDnsClient(this.dnsClient)
                    .get()
                    .uri(uri)
                    .addHeader("Host", uri.getHost())
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Connection", "close")
                    .build();

            HttpResponse response = httpClient.send(getRequest);

            shouldTry = handleResponse(uri, response);
        }
    }

    /**
     * @param uri
     * @param response is modified for a redirect if needed
     * @return
     */
    private boolean handleResponse(URI uri, HttpResponse response) {
        if (response.status() == 200) {
            PARSER_THREADS.execute(() -> parseSuccessfulResponse(uri, response));
            return false;
        } else if (response.status() == 301) {
            // TODO: handle permanent redirect
            return true;
        } else if (response.status() % 100 == 3){
            // TODO: handle other
            return true;
        } else {
            LOGGER.warn("Got status {} on URI {}", response.status(), uri);
            return false;
        }
    }

    private void parseSuccessfulResponse(URI uri, HttpResponse response) {
        LOGGER.info("Parsing document at URI {} at {}", uri, Instant.now());

        Document htmlDoc = Jsoup.parse(response.getBody(), uri.toString());
        Elements links = htmlDoc.select("a[href]");

        Element metaRobotsTag = htmlDoc.selectFirst("meta[name='robots']");
        Set<String> instructions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (metaRobotsTag != null) {


            String metaRobotsTagContent = metaRobotsTag.attr("content");
            for (String str : metaRobotsTagContent.split(",")) {
                instructions.add(str.trim());
            }
        }

        if (shouldFollowLinks(instructions)) {
            followLinks(uri, links);
        }

        if (shouldIndex(instructions)) {
            saveDocument(uri, htmlDoc);
        }
    }

    private boolean shouldFollowLinks(Set<String> instructions) {
        return !instructions.contains("nofollow") && !instructions.contains("none");
    }

    private boolean shouldIndex(Set<String> instructions) {
        return !instructions.contains("noindex") && !instructions.contains("none");
    }

    private void followLinks(URI uri, Elements links) {
        for (Element link : links) {
            try {
                String str = link.absUrl("href");
                if (!str.isEmpty()) {
                    URI href = new URI(str);
                    href = new URI(href.getScheme(), href.getHost(), href.getPath(), href.getQuery(), null);

                    if (isWorthFollowingLocally(href)) {
                        this.downloadQueue.add(href);
                    } else {
                        this.controller.add(href);
                    }
                }
            } catch (URISyntaxException e) {
                //LOGGER.info("Reconstructed URI was malformed {}", e);
            }
        }
    }

    private boolean isWorthFollowingLocally(URI next) {
        return rules.isAllowed(next.toString()) && !hasBeenVisited(next) && next.getHost().equals(this.targetHost);
    }

    private void saveDocument(URI uri, org.jsoup.nodes.Document jsoupDoc) {
        Path fileSystemTranslatedPath = translateToPath(uri);

        BufferedWriter bw = null;
        try {
            Files.createDirectories(fileSystemTranslatedPath.getParent());
            bw = Files.newBufferedWriter(fileSystemTranslatedPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            bw.write(jsoupDoc.html());
        } catch (FileAlreadyExistsException e) {
            LOGGER.warn("Ignoring and not overwriting an existing file {}", fileSystemTranslatedPath);
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

        String host = uri.getHost();
        String path = uri.getPath();
        String query = uri.getQuery();

        if (host != null) {
            host = escapeUriCharacters(host);
        }

        if (path != null) {
            path = escapeUriCharacters(path);
        }

        if (query != null) {
            query = escapeUriCharacters(path);
        }

        // Construct path path
        Path pathOfUri = Paths.get(controller.getDestination().toString(), host);
        if (path != null) {
            pathOfUri = Paths.get(pathOfUri.toString(), path);
        }

        if (isMissingIndex(uri)) {
            pathOfUri = Paths.get(pathOfUri.toString(), "index.html");
        }

        if (query != null) {
            pathOfUri = Paths.get(pathOfUri.toString(), query);
        }

        if (isMissingExtension((pathOfUri))) {
            pathOfUri = Paths.get(pathOfUri.toString() + ".html");
        }

        return pathOfUri;
    }

    private String escapeUriCharacters(String str) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (ESCAPE_SEQUENCES.containsKey(c)) {
                builder.append(ESCAPE_SEQUENCES.get(c));
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private boolean isMissingIndex(URI uri) {
        URI auxUri = null;
        try {
            auxUri = new URI("http", uri.getHost(), null, null);
        } catch (URISyntaxException e) {
            LOGGER.warn("URL was malformed for {}", uri);
        }

        return uri.equals(auxUri) || uri.toString().endsWith("/");
    }

    private boolean isMissingExtension(Path path) {
        String extension = com.google.common.io.Files.getFileExtension(path.toString());
        return extension.isEmpty();
    }

    public BaseRobotRules getRules() {
        return rules;
    }
}
