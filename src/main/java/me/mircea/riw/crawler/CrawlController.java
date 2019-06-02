package me.mircea.riw.crawler;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.BaseRobotsParser;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import me.mircea.riw.http.HttpClient;
import me.mircea.riw.http.HttpRequest;
import me.mircea.riw.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class CrawlController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlController.class);
    //private static final String USER_AGENT = "Mozilla/5.0 (compatible; dmcBot/1.0)";
    private static final String USER_AGENT = "RIWEB_CRAWLER";
    private static final long DEFAULT_CRAWL_DELAY = 1000;


    private final BlockingQueue<URI> activeQueue;
    private final ScheduledExecutorService scheduler;

    private final Map<String, BaseRobotRules> hostRobotRules;
    private final Map<String, Fetcher> hostFetchers;
    private final Map<Fetcher, ScheduledFuture<?>> fetcherTasks;
    private final Path destination;

    public CrawlController(int noThreads, Path destination) {
        this.activeQueue = new ActiveDownloadQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(noThreads);

        this.hostRobotRules = new HashMap<>();
        this.hostFetchers = new HashMap<>();
        this.fetcherTasks = new HashMap<>();
        this.destination = destination;
    }

    public void add(URI uri) {
        activeQueue.add(uri);
    }

    public void addAll(Collection<URI> uris) {
        activeQueue.addAll(uris);
    }

    public Path getDestination() {
        return destination;
    }

    public void shutdownFetcher(Fetcher fetcher) {
        ScheduledFuture<?> task = fetcherTasks.remove(fetcher);
        if (!task.isDone()) {
            //boolean isCancelled =
            task.cancel(true);
        }

        task.cancel(true);

        hostFetchers.remove(fetcher.getTargetHost(), fetcher);
    }

    public void crawl() throws InterruptedException {
        while (true) {
            URI uri = activeQueue.poll(1, TimeUnit.MINUTES);

            if (uri != null) {
                Optional<Fetcher> hostFetcher = getHostFetcherFor(uri);
                if (hostFetcher.isPresent()) {
                    Fetcher fetcher = hostFetcher.get();
                    if (fetcher.getRules().isAllowed(uri.toString())) {
                        hostFetcher.get().add(uri);
                    }
                }
            }
        }
    }
    /**
     * @brief Upsert a new fetcher if possible
     */
    private Optional<Fetcher> getHostFetcherFor(URI uri) {
        if (hostFetchers.containsKey(uri.getHost())) {
            return Optional.of(hostFetchers.get(uri.getHost()));
        } else {
            Fetcher hostFetcher = null;
            try {
                hostFetcher = new Fetcher(this, uri, getHostRobotRulesFrom(uri));
                hostFetchers.put(uri.getHost(), hostFetcher);

                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new FetcherTask(hostFetcher),
                        0,
                        hostFetcher.getRules().getCrawlDelay(),
                        TimeUnit.MILLISECONDS);

                fetcherTasks.put(hostFetcher, future);
            } catch (SocketException | UnknownHostException e) {
                LOGGER.info("Could not establish connection to seed {}", uri);
            }

            return Optional.ofNullable(hostFetcher);
        }
    }

    /**
     * @brief Upsert new robot rules (get default with no disallow and 1 sec crawl delay)
     */
    private BaseRobotRules getHostRobotRulesFrom(URI uri) {
        if (hostRobotRules.containsKey(uri.getHost())) {
            return hostRobotRules.get(uri.getHost());
        } else {
            BaseRobotRules rules = new SimpleRobotRules();

            try {
                URI robotsTxtURI = new URI(uri.getScheme(), uri.getHost(), "/robots.txt", null);
                BaseRobotsParser robotsParser = new SimpleRobotRulesParser();

                HttpClient client = new HttpClient();
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .get()
                        .uri(robotsTxtURI)
                        .addHeader("Host", robotsTxtURI.getHost())
                        .addHeader("User-Agent", USER_AGENT)
                        .addHeader("Connection", "close")
                        .build();

                HttpResponse repResponse = client.send(getRequest);

                if (repResponse.status() == 200) {
                    rules = robotsParser.parseContent(robotsTxtURI.toString(),
                            repResponse.getBody().getBytes(),
                            "text/plain",
                            USER_AGENT);
                } else {
                    LOGGER.info("Got status {} on robots.txt file at URI {}", repResponse.status(), robotsTxtURI);
                }
            } catch (URISyntaxException | IOException e) {
                LOGGER.warn("Exception occured while trying to get robots.txt on {}", uri.getHost());
            }

            if (rules.getCrawlDelay() == BaseRobotRules.UNSET_CRAWL_DELAY) {
                rules.setCrawlDelay(DEFAULT_CRAWL_DELAY);
            }

            return rules;
        }
    }
}
