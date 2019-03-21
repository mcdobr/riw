package me.mircea.riw;

import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.indexer.MongoIndexer;
import me.mircea.riw.model.Document;
import me.mircea.riw.parser.TextParser;
import me.mircea.riw.parser.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    static final BlockingQueue<Path> PATH_QUEUE = new LinkedBlockingDeque<>();
    static final BlockingQueue<Document> DOCUMENT_QUEUE = new LinkedBlockingDeque<>();
    static final ExecutorService TASK_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    static final List<Future<?>> FUTURES = new ArrayList<>();


    static final int NO_TRAVERSERS = 2;
    static final int NO_INDEXERS = 2;

    public static void main(String[] args){
        if (args.length == 0) {
            displayHelp();
            System.exit(0);
        }

        switch (args[0].toLowerCase()) {
            case "index":
                setUpIndexing();

                for (int idx = 1; idx < args.length; ++idx) {
                    PATH_QUEUE.add(Paths.get(args[idx]));
                }

                blockUntilAllTasksAreDone();
                break;
            case "search":
                break;
            case "help":
            default:
                displayHelp();
        }
    }

    private static void displayHelp() {
        System.out.println(
                String.join("\n",
                        "sengine index <<path>> to index a folder",
                        "sengine search <<query>> to search in the current index",
                        "sengine help to display the help"));
    }

    private static void setUpIndexing() {
        for (int i = 0; i < NO_TRAVERSERS; ++i) {
            Future<?> future = TASK_EXECUTOR.submit(new Traverser(PATH_QUEUE, DOCUMENT_QUEUE, new TextParser()));
            FUTURES.add(future);
        }

        for (int i = 0; i < NO_INDEXERS; ++i) {
            Future<?> future = TASK_EXECUTOR.submit(new MongoIndexer(DOCUMENT_QUEUE, DatabaseManager.instance.getDatabase()));
            FUTURES.add(future);
        }
    }

    private static void blockUntilAllTasksAreDone() {
        for (Future<?> future: FUTURES) {
            try {
                future.get();
            } catch (InterruptedException ie) {
                LOGGER.error("Thread interrupted: {}", ie);
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                LOGGER.error("An exception occured inside the executor service: {}", ee);
            }
        }
    }
}
