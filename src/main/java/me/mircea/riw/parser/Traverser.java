package me.mircea.riw.parser;

import com.google.common.base.Preconditions;
import me.mircea.riw.indexer.AsyncQueueableIndexer;
import me.mircea.riw.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Traverser {
    private static final Logger LOGGER = LoggerFactory.getLogger(Traverser.class);

    private final List<AsyncQueueableIndexer> workers;
    private final Parser parser;
    private final Queue<Path> pathQueue;


    public Traverser(Path seed, List<AsyncQueueableIndexer> workers, Parser parser) {
        Preconditions.checkNotNull(seed);
        Preconditions.checkNotNull(workers);
        Preconditions.checkNotNull(parser);
        this.workers = workers;
        this.parser = parser;
        this.pathQueue = new LinkedList<>();
        this.pathQueue.add(seed);
    }

    public void traverse() throws IOException {
        int targetedWorker = 0;

        while (!pathQueue.isEmpty()) {
            Path path = pathQueue.poll();
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> dirContents = Files.newDirectoryStream(path)) {
                    dirContents.forEach(pathQueue::add);
                }
            } else if (Files.isRegularFile(path)) {
                Document doc = parser.parse(path);
                workers.get(targetedWorker).indexDocument(doc);
                targetedWorker = (targetedWorker + 1) % workers.size();
            }
        }
    }
}
