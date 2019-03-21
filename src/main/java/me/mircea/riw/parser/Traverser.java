package me.mircea.riw.parser;

import com.google.common.base.Preconditions;
import me.mircea.riw.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Traverser implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Traverser.class);

    private final BlockingQueue<Path> pathQueue;
    private final BlockingQueue<Document> documentQueue;
    private final Parser parser;

    public Traverser(BlockingQueue<Path> pathQueue, BlockingQueue<Document> documentQueue, Parser parser) {
        Preconditions.checkNotNull(pathQueue);
        Preconditions.checkNotNull(documentQueue);
        Preconditions.checkNotNull(parser);
        this.pathQueue = pathQueue;
        this.documentQueue = documentQueue;
        this.parser = parser;
    }

    @Override
    public void run() {
        try {
            loopWhileIncomingFiles();
        } catch (InterruptedException ie) {
            LOGGER.warn("Traverser thread was interrupted: {}", ie);
            Thread.currentThread().interrupt();
        }
    }

    private void loopWhileIncomingFiles() throws InterruptedException {
        while (true) {
            Path path;
            if ((path = pathQueue.poll(1, TimeUnit.SECONDS)) == null)
                break;

            try {
                handlePath(path);
            } catch (IOException ioe) {
                LOGGER.warn("Could not handle file {} with ex {}", path, ioe);
            }
        }
    }

    private void handlePath(final Path path) throws IOException {
        Preconditions.checkNotNull(path);
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> dirContents = Files.newDirectoryStream(path)) {
                dirContents.forEach(pathQueue::add);
            }
        } else if (Files.isRegularFile(path)) {
            documentQueue.add(parser.parse(path));
        }
    }
}
