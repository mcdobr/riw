package me.mircea.riw;

import com.google.common.base.Stopwatch;
import me.mircea.riw.crawler.CrawlController;
import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.indexer.AsyncQueueableIndexer;
import me.mircea.riw.indexer.MongoIndexer;
import me.mircea.riw.model.Document;
import me.mircea.riw.parser.TextParser;
import me.mircea.riw.parser.Traverser;
import me.mircea.riw.search.QuantitativeSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import static java.util.AbstractMap.*;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final DatabaseManager DATABASE_MANAGER;
    private static final ExecutorService WORKER_EXECUTOR;
    private static final CompletionService<Boolean> COMPLETION_SERVICE;
    private static final List<AsyncQueueableIndexer> WORKERS;
    private static final int NO_THREADS;
    private static final int NO_WORKERS;
    private static final TimeUnit TIME_MEASUREMENT_UNIT;

    static {
        Properties config = new Properties();
        try {
            InputStream configStream = Main.class.getClassLoader().getResourceAsStream("config.properties");
            config.load(configStream);
        } catch (IOException ie) {
            LOGGER.error("Could not read config file. Exiting abnormally");
            System.exit(-1);
        }

        DATABASE_MANAGER = new DatabaseManager(config.getProperty("dbConnection"));
        NO_THREADS = Integer.parseInt(config.getProperty("noThreads"));
        NO_WORKERS = Integer.parseInt(config.getProperty("noWorkers"));
        TIME_MEASUREMENT_UNIT = TimeUnit.valueOf(config.getProperty("timeMeasurementUnit").toUpperCase());

        WORKER_EXECUTOR = Executors.newFixedThreadPool(NO_THREADS);
        COMPLETION_SERVICE = new ExecutorCompletionService<>(WORKER_EXECUTOR);

        WORKERS = new ArrayList<>();
        for (int i = 0; i < NO_WORKERS; ++i) {
            MongoIndexer worker = new MongoIndexer(DATABASE_MANAGER, new TextParser());
            WORKERS.add(worker);
            COMPLETION_SERVICE.submit(worker);
        }
    }

    public static void main(String[] args){
        if (args.length == 0) {
            displayHelp();
            System.exit(0);
        }

        LOGGER.info("Started at {}", Instant.now());
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            switch (args[0].toLowerCase()) {
                case "crawl":
                    CrawlController controller = new CrawlController(1, Paths.get("C:/Users/mircea/Desktop/dest/"));

                    try {
                        controller.add(new URI("http://riweb.tibeica.com/"));
                        //controller.add(new URI("http://www.ace.tuiasi.ro/"));
                        //controller.add(new URI("http://www.etti.tuiasi.ro/"));
                    } catch (URISyntaxException e) {
                        LOGGER.warn("A URI was malformed {}", e);
                    }
                    controller.crawl();

                    break;
                case "index":
                    Traverser traverser = new Traverser(Paths.get(args[1]), WORKERS);
                    traverser.traverse();
                    break;
                case "clean":
                    DATABASE_MANAGER.clean();
                    break;
                case "search":
                    QuantitativeSearcher searcher = new QuantitativeSearcher(DATABASE_MANAGER);
                    List<SimpleImmutableEntry<Document, Double>> searchResults = searcher.search(args[1]);

                    System.out.println(args[1]);
                    System.out.printf("%d documents found %n", searchResults.size());
                    searchResults.forEach(kvp -> System.out.printf("%s: %f%n", kvp.getKey(), kvp.getValue()));
                    break;
                case "help":
                default:
                    displayHelp();
            }
        } catch (IOException ie) {
            LOGGER.error("File system related error: {}", ie);
        }
        stopWorkers();

        stopwatch.stop();
        LOGGER.info("{} threads, {} workers", NO_THREADS, NO_WORKERS);
        LOGGER.info("Elapsed time: {} {}", stopwatch.elapsed(TIME_MEASUREMENT_UNIT), TIME_MEASUREMENT_UNIT);
        LOGGER.info("Ended at {}", Instant.now());
    }

    private static void displayHelp() {
        System.out.println(
                String.join("\n",
                        "sengine index <<path>> to index a folder",
                        "sengine search \"<<query>>\" to search in the current index",
                        "sengine help to display the help"));
    }

    private static void stopWorkers() {
        for (AsyncQueueableIndexer worker : WORKERS) {
            worker.stop();
        }
        WORKER_EXECUTOR.shutdown();
        LOGGER.info("Sending stop signal to workers...");

        try {
            for (int i = 0; i < NO_WORKERS; ++i) {
                Future<?> future = COMPLETION_SERVICE.take();
                future.get();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted {}", e.getCause());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("Execution exception caused by {}", e.getCause());
        }
    }
}
