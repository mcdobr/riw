package me.mircea.riw.indexer;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoDatabase;
import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.model.Document;
import me.mircea.riw.model.Term;
import me.mircea.riw.model.TermLink;
import me.mircea.riw.parser.TextParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MongoIndexer implements AsyncQueueableIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoIndexer.class);
    private static final Path POISON_PILL = Paths.get("poison_pill");

    private final BlockingQueue<Path> documentQueue;
    private final TextParser parser;
    private final DatabaseManager dbManager;

    public MongoIndexer(DatabaseManager dbManager, TextParser parser) {
        Preconditions.checkNotNull(dbManager);
        Preconditions.checkNotNull(parser);
        this.dbManager = dbManager;
        this.parser = parser;
        this.documentQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public Boolean call() {
        try {
            while (true) {
                Path path = documentQueue.take();
                if (path == POISON_PILL)
                    break;

                try {
                    Document doc = parser.parse(path);
                    indexDocument(doc);
                } catch (IOException ie) {
                    LOGGER.warn("Could not read path {}", ie);
                }
            }
        } catch (InterruptedException ie) {
            LOGGER.warn("Indexer thread was interrupted: {}", ie);
            Thread.currentThread().interrupt();
        }
        return true;
    }

    @Override
    public void indexDocument(Document doc) {
        LOGGER.info("Indexing document {}", doc.getAbsUrl());
        indexDirect(doc);
        indexInverted(doc);
    }

    private void indexDirect(Document doc) {
        dbManager.upsertDocument(doc);
    }

    private void indexInverted(Document doc) {
        doc.getTerms().entrySet()
                .stream()
                .forEach(entry -> {
                    Term term = new Term(entry.getKey());
                    term.getDocumentFrequency().add(new TermLink(doc, entry.getValue()));

                    dbManager.upsertTerm(term);
                });
    }

    @Override
    public void scheduleDocument(Path path) {
        Preconditions.checkNotNull(path);
        documentQueue.add(path);
    }

    @Override
    public void stop() {
        scheduleDocument(POISON_PILL);
    }
}
