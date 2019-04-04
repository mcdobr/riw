package me.mircea.riw;

import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.indexer.MongoIndexer;
import me.mircea.riw.parser.TextParser;
import me.mircea.riw.parser.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    static final ExecutorService WORKER_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    //static final List<Future<?>> FUTURES = new ArrayList<>();

    public static void main(String[] args){
        if (args.length == 0) {
            displayHelp();
            System.exit(0);
        }

        switch (args[0].toLowerCase()) {
            case "index":
                setUpIndexing();
                Traverser traverser = new Traverser(Paths.get(args[1]), WORKER_EXECUTOR, new TextParser());
                traverser.traverse();


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
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            MongoIndexer worker = new MongoIndexer(DatabaseManager.instance.getDatabase());
            WORKER_EXECUTOR.submit(worker);
        }
    }
}
