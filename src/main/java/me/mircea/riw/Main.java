package me.mircea.riw;

import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.indexer.AsyncQueueableIndexer;
import me.mircea.riw.indexer.MongoIndexer;
import me.mircea.riw.parser.TextParser;
import me.mircea.riw.parser.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    static final ExecutorService WORKER_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    static final List<AsyncQueueableIndexer> WORKERS = new ArrayList<>();
    static final DatabaseManager dbManager = DatabaseManager.getInstance();


    static {
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            MongoIndexer worker = new MongoIndexer();
            WORKERS.add(worker);
            WORKER_EXECUTOR.submit(worker);
        }
    }

    public static void main(String[] args){
        if (args.length == 0) {
            displayHelp();
            System.exit(0);
        }

        try {
            switch (args[0].toLowerCase()) {
                case "index":
                    Traverser traverser = new Traverser(Paths.get(args[1]), WORKERS, new TextParser());
                    traverser.traverse();
                    break;
                case "clean":
                    dbManager.clean();
                    break;
                case "search":
                    break;
                case "help":
                default:
                    displayHelp();
            }
        } catch (IOException ie) {
            LOGGER.error("File system related error: {}", ie);
        }

        stopWorkers();

    }

    private static void displayHelp() {
        System.out.println(
                String.join("\n",
                        "sengine index <<path>> to index a folder",
                        "sengine search <<query>> to search in the current index",
                        "sengine help to display the help"));
    }

    private static void stopWorkers() {
        for (AsyncQueueableIndexer worker : WORKERS) {
            worker.stop();
        }
        WORKER_EXECUTOR.shutdown();
    }
}
