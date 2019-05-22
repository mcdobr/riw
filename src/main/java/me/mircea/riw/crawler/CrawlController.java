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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class CrawlController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlController.class);
    private static final String BOT_NAME = "dmcBot";
    private static final long DEFAULT_CRAWL_DELAY = 1000;


    private final BlockingQueue<URI> activeQueue;
    private final ExecutorService executor;
    private final Map<String, BaseRobotRules> hostRobotRules;
    private final Map<String, Fetcher> hostFetchers;
    private final Path destination;

    public CrawlController(int noThreads, Path destination) {
        this.activeQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newFixedThreadPool(noThreads);
        this.hostRobotRules = new HashMap<>();
        this.hostFetchers = new HashMap<>();
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

    public void crawl() {
        while (true) {
            URI uri = activeQueue.poll();

            if (uri != null) {
                BaseRobotRules relevantRules = getHostRobotRulesFrom(uri);
                if (relevantRules.isAllowed(uri.toString())) {
                    Fetcher hostFetcher = getHostFetcherFor(uri);
                    hostFetcher.fetch(uri);
                }
            }
        }
    }

    /**
     * @brief Get (and insert if needed) the robots ruled associated with host
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
                        .addHeader("User-Agent", "Mozilla/5.0 (compatible; dmcBot/1.0)")
                        .addHeader("Connection", "close")
                        .build();

                HttpResponse repResponse = client.send(getRequest);

                if (repResponse.status() == 200) {
                    rules = robotsParser.parseContent(robotsTxtURI.toString(),
                            repResponse.getBody().getBytes(),
                            "text/plain",
                            BOT_NAME);
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not find robots.txt file on domain {}. Exception {}", uri.getHost(), e);
            } catch (UnknownHostException e) {
                LOGGER.warn("Could not reach the host {}, Exception {}", uri.getHost(), e);
            } catch (IOException e) {
                LOGGER.warn("An I/O error occured {}", e);
            }

            if (rules.getCrawlDelay() == BaseRobotRules.UNSET_CRAWL_DELAY) {
                rules.setCrawlDelay(DEFAULT_CRAWL_DELAY);
            }

            return rules;
        }
    }

    /**
     * @brief Get the fetcher task or create one
     */
    private Fetcher getHostFetcherFor(URI uri) {
        if (hostFetchers.containsKey(uri.getHost())) {
            return hostFetchers.get(uri.getHost());
        } else {
            Fetcher hostFetcher = new Fetcher(this, uri);
            hostFetchers.put(uri.getHost(), hostFetcher);
            executor.submit(hostFetcher);

            return hostFetcher;
        }
    }
}
