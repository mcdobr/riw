package me.mircea.riw.indexer;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoDatabase;
import me.mircea.riw.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MongoIndexer implements Runnable, Indexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoIndexer.class);

    private final BlockingQueue<Document> documentQueue;
    private final MongoDatabase database;

    public MongoIndexer(BlockingQueue<Document> documentQueue, MongoDatabase database) {
        Preconditions.checkNotNull(documentQueue);
        Preconditions.checkNotNull(database);
        this.documentQueue = documentQueue;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Document doc;
                if ((doc = documentQueue.poll(1, TimeUnit.SECONDS)) == null)
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
        Preconditions.checkNotNull(doc);




        throw new UnsupportedOperationException("Not implemented yet");
    }
}
