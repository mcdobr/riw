package me.mircea.riw.indexer;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoDatabase;
import me.mircea.riw.model.Document;
import me.mircea.riw.parser.TextParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MongoIndexer implements Indexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoIndexer.class);
    private static final Document POISON_PILL = new Document();

    private final BlockingQueue<Document> documentQueue;
    private final MongoDatabase database;

    public MongoIndexer(MongoDatabase database) {
        Preconditions.checkNotNull(database);
        this.database = database;
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
        Preconditions.checkNotNull(doc);


        TextParser parser = new TextParser();
        Map<String, Integer> stemOccurences = parser.extractWordStems(doc.getText());

        indexDirect(stemOccurences);
        indexInverted();

    }

    private void indexDirect(Map<String, Integer> stemOccurences) {
        //MongoIndexer.in
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void indexInverted() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
