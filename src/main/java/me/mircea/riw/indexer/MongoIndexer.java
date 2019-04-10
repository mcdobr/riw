package me.mircea.riw.indexer;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoDatabase;
import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.model.Document;
import me.mircea.riw.model.Term;
import me.mircea.riw.model.TermLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MongoIndexer implements AsyncQueueableIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoIndexer.class);
    private static final Document POISON_PILL = new Document();

    private final BlockingQueue<Document> documentQueue;
    private final DatabaseManager dbManager;

    public MongoIndexer(DatabaseManager dbManager) {
        Preconditions.checkNotNull(dbManager);
        this.dbManager = dbManager;
        this.documentQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Document doc = documentQueue.take();
                if (doc == POISON_PILL)
                    break;

                indexDocument(doc);
            }
        } catch (InterruptedException ie) {
            LOGGER.warn("Indexer thread was interrupted: {}", ie);
            Thread.currentThread().interrupt();
        }
    }
    @Override
    public void indexDocument(Document doc) {
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
    public void scheduleDocument(Document doc) {
        Preconditions.checkNotNull(doc);
        documentQueue.add(doc);
    }

    @Override
    public void stop() {
        scheduleDocument(POISON_PILL);
    }
}
